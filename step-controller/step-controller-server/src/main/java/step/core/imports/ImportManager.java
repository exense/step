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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.exense.commons.io.FileHelper;
import step.core.GlobalContext;
import step.core.Version;
import ch.exense.commons.core.model.accessors.AbstractIdentifiableObject;
import ch.exense.commons.core.accessors.Accessor;
import ch.exense.commons.core.accessors.DefaultJacksonMapperProvider;
import ch.exense.commons.core.collections.Collection;
import ch.exense.commons.core.collections.Document;
import ch.exense.commons.core.collections.Filters;
import ch.exense.commons.core.collections.filesystem.FilesystemCollectionFactory;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.migration.MigrationContext;
import step.migration.MigrationManager;
import step.resources.LocalResourceManagerImpl;

public class ImportManager {
	
	private static final Logger logger = LoggerFactory.getLogger(ImportServices.class);
	protected final GlobalContext context;
	protected final EntityManager entityManager;
	private final ObjectMapper mapper = DefaultJacksonMapperProvider.getObjectMapper();
	private FilesystemCollectionFactory tempCollectionFactory;
	
	public ImportManager(GlobalContext globalContext) throws IOException {
		super();
		this.context = globalContext;
		entityManager = context.getEntityManager();
		tempCollectionFactory = new FilesystemCollectionFactory(FileHelper.createTempFolder());
	}

	/** import entities included in provided file
	 * @param importConfig import config object with all required details (source file, enricher...)
	 * @throws Exception
	 */
	public void importAll(ImportConfiguration importConfig) throws Exception {
		File archiveFile = importConfig.getFile();
		if (FileHelper.isArchive(archiveFile)) {
			File tmpFolder = new File("import"+ UUID.randomUUID());
			importConfig.setLocalResourceMgr(new LocalResourceManagerImpl(tmpFolder));
			FileHelper.unzip(archiveFile,tmpFolder);
			File jsonFile = new File(tmpFolder.getPath()+"/export.json");
			Version version=null;
			try (JsonParser jParser = mapper.getFactory().createParser(jsonFile)) {
				if (jParser.nextToken() == JsonToken.START_OBJECT && jParser.nextToken() == JsonToken.FIELD_NAME) {
					String name = jParser.getCurrentName();
					//All exported JSON files starting from version 3.14 starts with a "metadata" field 
					if (name.contentEquals("metadata")) {
						//Read the metadata
						jParser.nextToken();
						TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {};
						Map<String,String> metadata = mapper.readValue(jParser, typeRef);
						if (metadata.containsKey("version")) {
							version = new Version(metadata.get("version"));
						}
						logger.info("Importing json from file: " + archiveFile.getName() + " with following metadata: " + metadata);
						importConfig.setVersion(version);
						importConfig.setMetadata(metadata);
						while (jParser.nextToken() != JsonToken.END_OBJECT) { 
							importEntitiesByType(importConfig, jParser, version);
						}
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
			} finally {
				if (importConfig.getLocalResourceMgr() != null) {
					importConfig.getLocalResourceMgr().cleanup();
				}
			}
		//json files are older plan export (plans for 3.13, arterfacts for previous versions)
		} else {
			importOlderPlans(importConfig);
		}
	}

	protected boolean skipEntityType(List<String> entitiesFilter, String entityName) {
		return (entitiesFilter!=null && !entitiesFilter.contains(entityName));
	}

	private void importEntitiesByType(ImportConfiguration importConfig, JsonParser jParser, Version fromVersion) throws IOException, InstantiationException, IllegalAccessException {
		String name = jParser.getCurrentName();
		
		Entity<?, ?> entityByName = entityManager.getEntityByName(name);
		boolean skip = skipEntityType(importConfig.getEntitiesFilter(), name);
		
		Collection<Document> tempCollection = tempCollectionFactory.getCollection(name, Document.class);
		
		Importer importer = new GenericImporter(importConfig, tempCollection, mapper);
		if (!skip) {
			if (entityByName == null) {
				throw new RuntimeException("The entity type with name '" + name + "' is unsupported in this version or license of step.");
			}
			logger.info("Importing entities of type " + name );
			if (jParser.nextToken().equals(JsonToken.START_ARRAY)) {
				while (!jParser.nextToken().equals(JsonToken.END_ARRAY)) {
					importer.importOne(jParser);
				}
			} else {
				throw new RuntimeException("A JSON array was expected for entity '" + name +"'");
			}
		} else {
			//consume the json object when skipped
			// TODO: fix this
			//mapper.readValue(jParser, BasicDBObject.class);
		}
		
		importFromTempCollection(tempCollection, importConfig, entityByName);
	}

	public void importFromTempCollection(Collection<Document> tempCollection, ImportConfiguration importConfig, Entity<?, ?> entityByName) {
		
		
		MigrationManager migrationManager = context.require(MigrationManager.class);
		migrationManager.migrate(tempCollectionFactory, importConfig.getVersion(), context.getCurrentVersion(),
				new MigrationContext(context.getRepositoryObjectManager()));

		Accessor<AbstractIdentifiableObject> accessor = (Accessor<AbstractIdentifiableObject>) entityByName.getAccessor();
		tempCollection.find(Filters.empty(), null, null, null, 0).forEach(document -> {
			AbstractIdentifiableObject entity = mapper.convertValue(document, entityByName.getEntityClass());
			entityManager.updateReferences(entity, importConfig.getReferences());
			// save the entity before running the import hooks. this is needed because of
			// the ResourceImporter relies on the ResourceManager that is backed by the
			// ResourceAccessor of the GlobalContext. Remove this as soon as the
			// ResourceImporter doesn't need it anymore
			accessor.save(entity);
			// import hooks
			entityManager.runImportHooks(entity, importConfig);
			// save again after having applied the import hooks
			accessor.save(entity);
		});
	}
	
	private void importOlderPlans(ImportConfiguration importConfig) throws InstantiationException, IllegalAccessException, IOException {
		String firstKey="undef";
		String firstValue="undef";
		//Extract class to determine version
		try (JsonParser jParser = mapper.getFactory().createParser(importConfig.getFile())) {
			if (jParser.nextToken() == JsonToken.START_OBJECT && jParser.nextToken() == JsonToken.FIELD_NAME) {
				firstKey = jParser.getCurrentName();
				jParser.nextToken();
				firstValue = jParser.getValueAsString();
			}
		}
		Entity<AbstractIdentifiableObject, Accessor<AbstractIdentifiableObject>> entity;
		if (firstKey.equals("_class") && !skipEntityType(importConfig.getEntitiesFilter(),EntityManager.plans)) {
			Version version;
			if (firstValue.startsWith("step.")) {
				version = new Version(3,13,0);
			} else {
				version = new Version(3,12,0);
			}
			importConfig.setVersion(version);
			logger.info("Importing file: " + importConfig.getFile().getName() + ". The file has no metadata, version detected: " + version.toString());
			
			Collection<Document> tempCollection = tempCollectionFactory.getCollection("plans", Document.class);
			Importer importer = new GenericImporter(importConfig, tempCollection, mapper);
			try(BufferedReader reader = Files.newBufferedReader(importConfig.getFile().toPath())) {
				String line;
				while((line=reader.readLine())!=null) {
					try (JsonParser jParser = mapper.getFactory().createParser(line)){
						jParser.nextToken();
						importer.importOne(jParser);		
					} catch (Exception e) {
						throw e;
					}
				}
			} catch (Exception e) {
				logger.error("Failed to import plan from version 3.13",e);
				throw new RuntimeException("Unable to import the plan, check the error logs for more details.",e);
			}

			importFromTempCollection(tempCollection, importConfig, entityManager.getEntityByName(EntityManager.plans));
		} else {
			logger.error("Import failed, the first property was unexpected '" + firstKey + "':'" + firstValue + "'");
			throw new RuntimeException("Import failed, the first property was unexpected '" + firstKey + "':'" + firstValue + "'"
					+ ". Check the error logs for more details.");
		}
		
	}

}
