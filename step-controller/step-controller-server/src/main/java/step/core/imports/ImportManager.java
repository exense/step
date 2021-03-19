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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mongodb.BasicDBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.exense.commons.io.FileHelper;
import step.core.GlobalContext;
import step.core.Version;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.export.ImportExportMapper;
import step.resources.LocalResourceManagerImpl;

public class ImportManager {
	
	private static final Logger logger = LoggerFactory.getLogger(ImportServices.class);
	protected final GlobalContext context;
	
	public ImportManager(GlobalContext globalContext) {
		super();
		this.context = globalContext;
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
			ObjectMapper mapper = ImportExportMapper.getMapper(context.getCurrentVersion());
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
						//Read next field which must be an entity type
						//Change mapper for given version
						mapper = ImportExportMapper.getMapper(version);
						Map<String, String> refMapping = new HashMap<String,String> ();
						while (jParser.nextToken() != JsonToken.END_OBJECT) { 
							importEntitiesByType(importConfig, jParser, mapper, refMapping);
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

	private void importEntitiesByType(ImportConfiguration importConfig, JsonParser jParser, ObjectMapper mapper, 
			Map<String, String> refMapping) throws IOException, InstantiationException, IllegalAccessException {
		String name = jParser.getCurrentName();
		boolean skip = skipEntityType(importConfig.getEntitiesFilter(), name);
		Entity<?, ?> entityByName = context.getEntityManager().getEntityByName(name);
		Importer<?,?> importer = null;
		if (!skip) {
			if (entityByName == null || entityByName.getImporter() == null) {
				throw new RuntimeException("The entity type with name '" + name + "' is unsupported in this version or license of step.");
			} else {
				importer = entityByName.getImporter();
			}
			logger.info("Importing entities of type " + name );
		}
		if (jParser.nextToken().equals(JsonToken.START_ARRAY)) {
			while (!jParser.nextToken().equals(JsonToken.END_ARRAY)) {
				if (!skip) {
					importer.importOne(importConfig, jParser, mapper, refMapping);
				} else {
					//consume the json object when skipped
					mapper.readValue(jParser, BasicDBObject.class);
				}
			}
		} else {
			throw new RuntimeException("A JSON array was expected for entity '" + name +"'");
		}
	}
	
	
	private void importOlderPlans(ImportConfiguration importConfig) throws InstantiationException, IllegalAccessException, IOException {
		ObjectMapper mapper = ImportExportMapper.getMapper(context.getCurrentVersion());
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
		if (firstKey.equals("_class") && !skipEntityType(importConfig.getEntitiesFilter(),EntityManager.plans)) {
			Importer<?,?> importer = context.getEntityManager().getEntityByName(EntityManager.plans).getImporter();
			Version version = new Version(3,12,0);
			//3.13
			if (firstValue.startsWith("step.")) {
				version = new Version(3,13,0);
			}
			importConfig.setVersion(version);
			logger.info("Importing file: " + importConfig.getFile().getName() + ". The file has no metadata, version detected: " + version.toString());
			//Change mapper for given version
			mapper = ImportExportMapper.getMapper(version);
			importer.importMany(importConfig, mapper);
		} else {
			logger.error("Import failed, the first property was unexpected '" + firstKey + "':'" + firstValue + "'");
			throw new RuntimeException("Import failed, the first property was unexpected '" + firstKey + "':'" + firstValue + "'"
					+ ". Check the error logs for more details.");
		}
	}


}
