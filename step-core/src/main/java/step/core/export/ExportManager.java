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
package step.core.export;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.DefaultJacksonMapperProvider;
import ch.exense.commons.io.FileHelper;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.entities.EntityReferencesMap;
import step.core.objectenricher.ObjectPredicate;
import step.resources.ResourceManager;

public class ExportManager {

	private static Logger logger = LoggerFactory.getLogger(ExportManager.class);

	private final ObjectMapper mapper = DefaultJacksonMapperProvider.getObjectMapper();
	private final EntityManager entityManager;
	private final ResourceManager resourceManager;

	public ExportManager(EntityManager entityManager, ResourceManager resourceManager) {
		super();
		this.entityManager = entityManager;
		this.resourceManager = resourceManager;
	}

	public ExportResult exportById(ExportConfiguration exportConfig, String id)
			throws FileNotFoundException, IOException {
		return export(exportConfig, c -> {
			entityManager.getEntitiesReferences(exportConfig.getEntityType(), id,
					exportConfig.getObjectPredicate(), c.getReferences(), exportConfig.isRecursively());
		});
	}

	public ExportResult exportAll(ExportConfiguration exportConfig) throws FileNotFoundException, IOException {
		return export(exportConfig, c -> {
			entityManager.getEntitiesReferences(exportConfig.getEntityType(), exportConfig.getObjectPredicate(),
					exportConfig.isRecursively(), c.getReferences());
		});
	}

	public ExportResult export(ExportConfiguration exportConfig, Consumer<ExportContext> runnable)
			throws FileNotFoundException, IOException {
		ExportContext exportContext = new ExportContext(exportConfig);
		EntityReferencesMap references = exportContext.getReferences();
		ObjectPredicate objectPredicate = exportConfig.getObjectPredicate();

		runnable.accept(exportContext);

		List<String> additionalEntities = exportConfig.getAdditionalEntities();
		if (additionalEntities != null && additionalEntities.size() > 0) {
			additionalEntities.forEach(e -> entityManager.getEntitiesReferences(e, objectPredicate, false, references));
		}

		export(exportContext);
		exportContext.addMessages(references.getRefNotFoundWarnings());
		return new ExportResult(exportContext.getMessages());
	}

	private void exportEntityByIds(ExportContext exportContext, String entityName, JsonGenerator jGen) {
		EntityReferencesMap references = exportContext.getReferences();
		Entity<?, ?> entity = entityManager.getEntityByName(entityName);
		if (entity == null) {
			throw new RuntimeException("Entity of type " + entityName + " is not supported");
		}
		references.getReferencesByType(entityName).forEach(id -> {
			AbstractIdentifiableObject a = entity.getAccessor().get(id);
			if (a == null) {
				logger.warn("Referenced entity with id '" + id + "' and type '" + entityName + "' is missing");
				references.addReferenceNotFoundWarning(
						"Referenced entity with id '" + id + "' and type '" + entityName + "' is missing");
			} else {
				entityManager.runExportHooks(a, exportContext);
				try {
					jGen.writeObject(a);
				} catch (IOException e) {
					throw new RuntimeException(
							"Error while exporting entity of type " + entity.getName() + " with id:" + id, e);
				}
			}
		});
	}

	private void export(ExportContext exportContext) throws FileNotFoundException, IOException {
		ExportConfiguration exportConfig = exportContext.getExportConfig();
		EntityReferencesMap references = exportContext.getReferences();
		ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();

		// Export db content to JSON
		try (JsonGenerator jGen = mapper.getFactory().createGenerator(jsonStream, JsonEncoding.UTF8)) {
			// Header with metadata
			// pretty print
			jGen.useDefaultPrettyPrinter();
			jGen.writeStartObject();
			jGen.writeObjectField("metadata", exportConfig.getMetadata());
			// start a json array for each entity type
			references.getTypes().forEach(e -> {
				try {
					jGen.writeArrayFieldStart(e);
					exportEntityByIds(exportContext, e, jGen);
					jGen.writeEndArray();
				} catch (IOException e1) {
					throw new RuntimeException("Error while exporting entity of type " + e, e1);
				}
			});
			jGen.writeEndObject();// end export object
		}
		try (ZipOutputStream zos = new ZipOutputStream(exportConfig.getOutputStream())) {
			FileHelper.zipFile(zos, jsonStream, "export.json");
			// Export resources (files)
			List<String> resourceRef = references.getReferencesByType(EntityManager.resources);
			Entity<?, ?> resourceEntity = entityManager.getEntityByName(EntityManager.resources);
			if (resourceRef != null && resourceRef.size() > 0 && resourceEntity != null) {
				exportResources(zos, references, resourceEntity);
			}
		}
	}

	private void exportResources(ZipOutputStream zos, EntityReferencesMap references, Entity<?, ?> resourceEntity) {
		List<String> resourceRef = references.getReferencesByType(EntityManager.resources);
		resourceRef.forEach(r -> {
			File file = resourceManager.getResourceFile(r).getResourceFile();
			if (file.exists()) {
				FileHelper.zipFile(zos, file, resourceManager.getResourcesRootPath());
			} else {
				references.addReferenceNotFoundWarning(
						"Resource file with id '" + r + "' and name" + file.getName() + "' is missing");
			}
		});
	}
}
