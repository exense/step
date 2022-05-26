/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.imports;

import ch.exense.commons.io.FileHelper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Version;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.migration.MigrationManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportManager {

	private static final Logger logger = LoggerFactory.getLogger(ImportManager.class);

	private final EntityManager entityManager;
	private final ObjectMapper mapper = DefaultJacksonMapperProvider.getObjectMapper();
	private final MigrationManager migrationManager;

	public ImportManager(EntityManager entityManager, MigrationManager migrationManager) throws IOException {
		super();
		this.entityManager = entityManager;
		this.migrationManager = migrationManager;
	}

	/**
	 * Import entities included in provided file
	 * 
	 * @param importConfig import config object with all required details (source
	 *                     file, enricher...)
	 * @return the result of the import
	 * @throws Exception
	 */
	public ImportResult importAll(ImportConfiguration importConfig) throws Exception {
		try (ImportContext importContext = new ImportContext(importConfig)) {
			File archiveFile = importConfig.getFile();
			if (FileHelper.isArchive(archiveFile)) {
				File workFolder = importContext.getWorkFolder();
				FileHelper.unzip(archiveFile, workFolder);
				File jsonFile = new File(workFolder.getPath() + "/export.json");
				Version version = null;
				try (JsonParser jParser = mapper.getFactory().createParser(jsonFile)) {
					if (jParser.nextToken() == JsonToken.START_OBJECT && jParser.nextToken() == JsonToken.FIELD_NAME) {
						String name = jParser.getCurrentName();
						// All exported JSON files starting from version 3.14 starts with a "metadata"
						// field
						if (name.contentEquals("metadata")) {
							// Read the metadata
							jParser.nextToken();
							TypeReference<HashMap<String, String>> typeRef = new TypeReference<>() {};
							Map<String, String> metadata = mapper.readValue(jParser, typeRef);
							if (metadata.containsKey("version")) {
								version = new Version(metadata.get("version"));
							}
							logger.info("Importing json from file: " + archiveFile.getName()
									+ " with following metadata: " + metadata);
							importContext.setVersion(version);
							importContext.setMetadata(metadata);
							
							// First import all entities to the temporary collections
							List<String> entityNames = new ArrayList<>();
							while (jParser.nextToken() != JsonToken.END_OBJECT) {
								entityNames.add(importEntitiesToTemporaryCollection(importConfig, importContext, jParser));
							}

							importEntitiesFromTemporaryCollection(importConfig, importContext, entityNames);
						} else {
							throw new RuntimeException("Missing metadata in json file");
						}
					} else {
						logger.error("Import failed, the root element of the file is not a json oject");
						throw new RuntimeException("Import failed, the root element of the file is not a json oject"
								+ ". Check the error logs for more details.");
					}
				} catch (Exception e) {
					logger.error("Import failed for file: " + archiveFile.getName(), e);
					throw new RuntimeException("Import failed, check the controller logs for more details");
				}
			} else {
				importOlderPlans(importConfig, importContext);
			}
			return new ImportResult(importContext.getMessages());
		}
	}

	private void importEntitiesFromTemporaryCollection(ImportConfiguration importConfig, ImportContext importContext, List<String> entityNames) {
		// Perform migration tasks on temporary collections
		final CollectionFactory tempCollectionFactory = importContext.getTempCollectionFactory();
		migrationManager.migrate(tempCollectionFactory, importContext.getVersion(), Version.getCurrentVersion());

		// Replace IDs of all entities if overwriting is disabled
		boolean generateNewObjectIds = !importConfig.isOverwrite();
		if (generateNewObjectIds) {
			entityNames.forEach(entityName -> replaceIds(importContext, entityName));
		}

		// Then import them from the temporary collection
		entityNames.forEach(entityName -> importFromTempCollection(importContext, entityName, generateNewObjectIds));
	}

	private void replaceIds(ImportContext importContext, String entityName) {
		Collection<Document> collection = importContext.getTempCollectionFactory().getCollection(entityName, Document.class);
		final Map<String, String> references = importContext.getReferences();
		final Map<String, String> newToOldReferences = importContext.getNewToOldReferences();
		collection.find(Filters.empty(), null, null, null, 0).forEach(entity -> {
			String origId = entity.getId().toString();
			ObjectId objectId;
			// if the origId was already replaced, use the new one
			if (references.containsKey(origId)) {
				objectId = new ObjectId(references.get(origId));
			} else {
				objectId = new ObjectId();
				if(logger.isDebugEnabled()) {
					logger.debug("Replacing id of entity: entityName = " + entityName + ", previousId = " + origId + ", newId = " + objectId);
				}
			}
			entity.setId(objectId);
			references.put(origId, objectId.toHexString());
			newToOldReferences.put(objectId.toHexString(), origId);
			collection.remove(Filters.id(origId));
			collection.save(entity);
		});
	}

	private boolean skipEntityType(List<String> entitiesFilter, String entityName) {
		return (entitiesFilter != null && !entitiesFilter.contains(entityName));
	}

	private String importEntitiesToTemporaryCollection(ImportConfiguration importConfig, ImportContext importContext, JsonParser jParser)
			throws IOException {
		String name = jParser.getCurrentName();

		Entity<?, ?> entityByName = entityManager.getEntityByName(name);
		boolean skip = skipEntityType(importConfig.getEntitiesFilter(), name);

		CollectionFactory tempCollectionFactory = importContext.getTempCollectionFactory();
		Collection<Document> tempCollection = tempCollectionFactory.getCollection(name, Document.class);

		if (entityByName == null) {
			throw new RuntimeException(
					"The entity type with name '" + name + "' is unsupported in this version or license of step.");
		}
		if (jParser.nextToken().equals(JsonToken.START_ARRAY)) {
			if (!skip) {
				logger.info("Importing entities of type " + name);
			}
			while (!jParser.nextToken().equals(JsonToken.END_ARRAY)) {
				if (!skip) {
					importOne(tempCollection, jParser);
				} else {
					// consume the json object when skipped
					mapper.readValue(jParser, Document.class);
				}
			}
		} else {
			throw new RuntimeException("A JSON array was expected for entity '" + name + "'");
		}

		return name;
	}

	private void importOne(Collection<Document> tempCollection, JsonParser jParser)	throws IOException {
		Document o = mapper.readValue(jParser, Document.class);
		// Normalize ID field for previous versions of STEP
		if (o.containsKey("_id")) {
			o.put(AbstractOrganizableObject.ID, o.get("_id"));
			o.remove("_id");
		}
		tempCollection.save(o);
	}

	private void importFromTempCollection(ImportContext importContext, String entityName, boolean generateNewObjectIds) {
		Entity<?, ?> entityByName = entityManager.getEntityByName(entityName);

		@SuppressWarnings("unchecked")
		Accessor<AbstractIdentifiableObject> accessor = (Accessor<AbstractIdentifiableObject>) entityByName
				.getAccessor();
		Collection<?> collection = importContext.getTempCollectionFactory().getCollection(entityByName.getName(),
				entityByName.getEntityClass());
		collection.find(Filters.empty(), null, null, null, 0).forEach(document -> {
			AbstractIdentifiableObject entity = mapper.convertValue(document, entityByName.getEntityClass());
			if (generateNewObjectIds) {
				if(logger.isDebugEnabled()) {
					logger.debug("Updating references for entity: entityName = " + entityName + ", id " + entity.getId());
				}
				entityManager.updateReferences(entity, importContext.getReferences(), o -> true);

			}
			// save the entity before running the import hooks. this is needed because
			// the ResourceImporter relies on the ResourceManager that is backed by the
			// ResourceAccessor of the GlobalContext. Remove this as soon as the
			// ResourceImporter doesn't need it anymore
			accessor.save(entity);
			// import hooks
			entityManager.runImportHooks(entity, importContext);
			// save again after having applied the import hooks
			accessor.save(entity);
		});
	}

	private void importOlderPlans(ImportConfiguration importConfig, ImportContext importContext) throws IOException {
		String firstKey = "undef";
		String firstValue = "undef";
		// Extract class to determine version
		try (JsonParser jParser = mapper.getFactory().createParser(importConfig.getFile())) {
			if (jParser.nextToken() == JsonToken.START_OBJECT && jParser.nextToken() == JsonToken.FIELD_NAME) {
				firstKey = jParser.getCurrentName();
				jParser.nextToken();
				firstValue = jParser.getValueAsString();
			}
		}
		if (firstKey.equals("_class") && !skipEntityType(importConfig.getEntitiesFilter(), EntityManager.plans)) {
			String collectionName;
			Version version;
			if (firstValue.startsWith("step.")) {
				version = new Version(3, 13, 0);
				collectionName = "plans";
			} else {
				version = new Version(3, 12, 0);
				collectionName = "artefacts";
			}
			importContext.setVersion(version);
			logger.info("Importing file: " + importConfig.getFile().getName()
					+ ". The file has no metadata, version detected: " + version);

			Collection<Document> tempCollection = importContext.getTempCollectionFactory().getCollection(collectionName,
					Document.class);
			try (BufferedReader reader = Files.newBufferedReader(importConfig.getFile().toPath())) {
				String line;
				while ((line = reader.readLine()) != null) {
					try (JsonParser jParser = mapper.getFactory().createParser(line)) {
						jParser.nextToken();
						importOne(tempCollection, jParser);
					}
				}
			}

			importEntitiesFromTemporaryCollection(importConfig, importContext, List.of(EntityManager.plans));
		} else {
			logger.error("Import failed, the first property was unexpected '" + firstKey + "':'" + firstValue + "'");
			throw new RuntimeException("Import failed, the first property was unexpected '" + firstKey + "':'"
					+ firstValue + "'" + ". Check the error logs for more details.");
		}
	}
}
