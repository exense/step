package step.core.imports;

import java.io.File;
import java.io.IOException;
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
	 * @param file containing the content to import
	 * @param objectEnricher to enrich imported data with additional info (i.e. project...) 
	 * @param entitiesFilter list of entity types to import, if null all entities are imported 
	 * @param overwrite define whether to keep exported ids (i.e. will overwrite if re-imported in same controller)
	 * @throws Exception
	 */
	public void importAll(ImportConfiguration importConfig) throws Exception {
		File jsonFile = importConfig.file;
		if (FileHelper.isArchive(importConfig.file)) {
			File tmpFolder = new File("import"+ UUID.randomUUID());
			importConfig.setLocalResourceMgr(new LocalResourceManagerImpl(tmpFolder));
			FileHelper.unzip(importConfig.file,tmpFolder);
			jsonFile = new File(tmpFolder.getPath()+"/export.json");
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
						logger.info("Importing json from file: " + importConfig.file.getName() + " with following metadata: " + metadata);
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
				logger.error("Import failed for file: " + importConfig.file.getName(), e);
				throw e;
			} finally {
				if (importConfig.localResourceMgr != null) {
					importConfig.localResourceMgr.cleanup();
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
		boolean skip = skipEntityType(importConfig.entitiesFilter, name);
		Importer<?,?> importer = context.getEntityManager().getEntityByName(name).getImporter();
		if (!skip) {
			if (importer==null) {
				throw new RuntimeException("The entity type with name '" + name + "' is unsupported.");
			}
			logger.info("Importing entities of type " + name );
		}
		if (jParser.nextToken().equals(JsonToken.START_ARRAY)) {
			while (!jParser.nextToken().equals(JsonToken.END_ARRAY)) {
				if (!skip) {
					importer.importOne(importConfig, jParser, mapper, refMapping);
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
		try (JsonParser jParser = mapper.getFactory().createParser(importConfig.file)) {
			if (jParser.nextToken() == JsonToken.START_OBJECT && jParser.nextToken() == JsonToken.FIELD_NAME) {
				firstKey = jParser.getCurrentName();
				jParser.nextToken();
				firstValue = jParser.getValueAsString();
			}
		}
		if (firstKey.equals("_class") && !skipEntityType(importConfig.entitiesFilter,EntityManager.plans)) {
			Importer<?,?> importer = context.getEntityManager().getEntityByName(EntityManager.plans).getImporter();
			Version version = new Version(3,12,0);
			//3.13
			if (firstValue.startsWith("step.")) {
				version = new Version(3,13,0);
			}
			importConfig.setVersion(version);
			logger.info("Importing file: " + importConfig.file.getName() + ". The file has no metadata, version detected: " + version.toString());
			//Change mapper for given version
			mapper = ImportExportMapper.getMapper(version);
			importer.importMany(importConfig, mapper);
		} else {
			logger.error("Import failed, the first property was unexepected '" + firstKey + "':'" + firstValue + "'");
			throw new RuntimeException("Import failed, the first property was unexepected '" + firstKey + "':'" + firstValue + "'"
					+ ". Check the error logs for more details.");
		}
	}


}
