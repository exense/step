package step.core.imports;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.Version;
import step.core.entities.EntityManager;
import step.core.export.ImportExportMapper;
import step.core.objectenricher.ObjectEnricher;

public class ImportManager {
	
	private static final Logger logger = LoggerFactory.getLogger(ImportServices.class);
	protected final GlobalContext context;
	
	public ImportManager(GlobalContext globalContext) {
		super();
		this.context = globalContext;
	}

	public void importAll(File file, ObjectEnricher objectEnricher, List<String> entitiesFilter, boolean overwrite) throws Exception {
		ObjectMapper mapper = ImportExportMapper.getMapper(context.getCurrentVersion());
		Version version=null;
		try (JsonParser jParser = mapper.getFactory().createParser(file)) {
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
					logger.info("Importing file: " + file.getName() + " with following metadata: " + metadata);
					//Read next field which must be an entity type
					//Change mapper for given version
					mapper = ImportExportMapper.getMapper(version);
					Map<String, String> refMapping = new HashMap<String,String> ();
					while (jParser.nextToken() != JsonToken.END_OBJECT) { 
						importEntitiesByType(jParser, mapper, objectEnricher, entitiesFilter, version, refMapping, overwrite);
					}
				//File without metadata are older plan export (plans for 3.13, arterfacts for previous versions)
				} else {
					jParser.nextToken();
					String value = jParser.getValueAsString();
					jParser.close();
					importOlderPlans(file, objectEnricher, entitiesFilter, name, value, overwrite);
				}
			} else {
				logger.error("Import failed, the root element of the file is not a json oject");
				throw new RuntimeException("Import failed, the root element of the file is not a json oject"
						+ ". Check the error logs for more details.");
			}

		} catch (Exception e) {
			logger.error("Import failed for file: " + file.getName(), e);
			throw e;
		}
	}
	
	protected boolean skipEntityType(List<String> entitiesFilter, String entityName) {
		return (entitiesFilter!=null && !entitiesFilter.contains(entityName));
	}

	private void importEntitiesByType(JsonParser jParser, ObjectMapper mapper, ObjectEnricher objectEnricher, List<String> entitiesFilter, Version version, Map<String, String> refMapping, boolean overwrite) throws IOException, InstantiationException, IllegalAccessException {
		String name = jParser.getCurrentName();
		boolean skip = skipEntityType(entitiesFilter, name);
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
					importer.importOne(jParser, mapper, objectEnricher, version, refMapping, overwrite);
				}
			}
		} else {
			throw new RuntimeException("A JSON array was expected for entity '" + name +"'");
		}
	}
	
	private void importOlderPlans(File file, ObjectEnricher objectEnricher, List<String> entitiesFilter, String firstKey, String firstValue, boolean overwrite) throws InstantiationException, IllegalAccessException, IOException {
		if (firstKey.equals("_class") && !skipEntityType(entitiesFilter,EntityManager.plans)) {
			Importer<?,?> importer = context.getEntityManager().getEntityByName(EntityManager.plans).getImporter();
			Version version = new Version(3,12,0);
			//3.13
			if (firstValue.startsWith("step.")) {
				version = new Version(3,13,0);
			}
			logger.info("Importing file: " + file.getName() + ". The file has no metadata, version detected: " + version.toString());
			//Change mapper for given version
			ObjectMapper mapper = ImportExportMapper.getMapper(version);
			importer.importMany(file, mapper, objectEnricher, version, overwrite);
		} else {
			logger.error("Import failed, the first property was unexepected '" + firstKey + "':'" + firstValue + "'");
			throw new RuntimeException("Import failed, the first property was unexepected '" + firstKey + "':'" + firstValue + "'"
					+ ". Check the error logs for more details.");
		}
	}


}
