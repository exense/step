package step.core.imports;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.Version;
import step.core.imports.converter.ArtefactsToPlans;
import step.core.objectenricher.ObjectEnricher;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;

public class PlanImporter extends GenericDBImporter implements Importer {
	
	private static final Logger logger = LoggerFactory.getLogger(ImportServices.class);
	
	public void importOne(JsonParser jParser, ObjectMapper mapper, ObjectEnricher objectEnricher, Version version) throws JsonParseException, JsonMappingException, IOException {
		if (version.compareTo(new Version(3,13,0)) >= 0) {
			Plan plan = mapper.readValue(jParser, Plan.class);
			objectEnricher.accept(plan);
			accessor.save(plan);
		} else {
			saveToTmpCollection(mapper.readValue(jParser, Document.class));			
		}
	}

	//Import plans exported with versions 3.13 and before (line by line)
	public void importMany(File file, ObjectMapper mapper, ObjectEnricher objectEnricher, Version version) throws IOException {
		try(BufferedReader reader = Files.newBufferedReader(file.toPath())) {
			String line;
			while((line=reader.readLine())!=null) {
				try (JsonParser jParser = mapper.getFactory().createParser(line)){
					jParser.nextToken();
					importOne(jParser, mapper, objectEnricher, version);		
				} catch (Exception e) {
					throw e;
				}
			}
			finalizeImport(version, objectEnricher);
		} catch (Exception e) {
			logger.error("Failed to import plan from version 3.13",e);
			throw new RuntimeException("Unable to import the plan, check the error logs for more details.",e);
		} finally {
			if (getTmpCollection() != null) {
				getTmpCollection().drop();
			}
		}
	}
	
	
	protected void finalizeImport(Version version, ObjectEnricher objectEnricher) {
		if (version.compareTo(new Version(3,13,0)) < 0 && getTmpCollection() != null) {
			ArtefactsToPlans artefactsToPlans = new ArtefactsToPlans(getTmpCollection(), (PlanAccessor) accessor, objectEnricher);
			int errors = artefactsToPlans.migrateArtefactsToPlans();
			if (errors > 0) {
				throw new RuntimeException("Import of plan from previous versions failed. Check the error logs for more  details.");
			}
		} 
	}
}
