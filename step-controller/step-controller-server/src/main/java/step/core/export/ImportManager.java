package step.core.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.artefacts.CallPlan;
import step.core.artefacts.AbstractArtefact;
import step.core.deployment.JacksonMapperProvider;
import step.core.objectenricher.ObjectEnricher;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;

public class ImportManager {
	
	private static final Logger logger = LoggerFactory.getLogger(ImportServices.class);
	
	protected final PlanAccessor planAccessor;
	
	public ImportManager(PlanAccessor planAccessor) {
		super();
		this.planAccessor = planAccessor;
	}

	public void importPlans(File file, ObjectEnricher objectEnricher) throws IOException {
		ObjectMapper mapper = JacksonMapperProvider.createMapper();
		try(BufferedReader reader = Files.newBufferedReader(file.toPath())) {
			String line;
			while((line=reader.readLine())!=null) {
				Plan plan = mapper.readValue(line, Plan.class);
				objectEnricher.accept(plan);
				planAccessor.save(plan);
			}			
		} catch (Exception e) {
			//Might be a plan export prior to version 3.13
			//Try to migrate and import
			try {
				importArtefacts(file,objectEnricher);
			} catch (Exception e2) {
				logger.error("Failed to import plan in version 3.13+",e);
				logger.error("Failed to import plan in version 3.12 and before",e2);
				throw new RuntimeException("Unable to import the plan, check the error logs for more details");
			}
		}
	}
	
	public void importArtefacts(File file, ObjectEnricher objectEnricher) throws IOException {
		ObjectMapper mapper = JacksonMapperProvider.createMapper();
		try(BufferedReader reader = Files.newBufferedReader(file.toPath())) {
			String line;
			Map<String,AbstractArtefact> artefacts = new HashMap<String,AbstractArtefact>();
			Map<String,String[]> artefactsChilds = new HashMap<String,String[]>();
			Map<String,ObjectId> rootsToPlanIds = new HashMap<String,ObjectId>();
			Pattern pattern = Pattern.compile("\"childrenIDs\":\\[([^\\]]*)\\],*");
			while((line=reader.readLine())!=null) {
				//Remove deprecated attributes before using the object mapper
				String modifiedLine = line;
				modifiedLine = modifiedLine.replaceAll("\"childrenIDs\":\\[[^\\]]*\\],*", "");
				modifiedLine = modifiedLine.replaceAll("\"childrenIDs\":[^,$]*,*", "");
				modifiedLine = modifiedLine.replaceAll("\"root\":[^,$]*,*","");
				if (line.contains("\"_class\":\"CallPlan\"")) {
					modifiedLine = modifiedLine.replaceAll("\"artefactId\":","\"planId\":");
				}
				AbstractArtefact artefact = mapper.readValue(modifiedLine, AbstractArtefact.class);
				String artefactIdStr = artefact.getId().toString();
				artefacts.put(artefactIdStr, artefact);
				if (line.contains("\"root\":true")) {
					rootsToPlanIds.put(artefactIdStr,new ObjectId(artefactIdStr));
				}
				Matcher matcher = pattern.matcher(line);
				if (matcher.find() && !matcher.group(1).isEmpty() )
				{
					artefactsChilds.put(artefactIdStr, matcher.group(1).replaceAll("\"", "").split(","));
				}
			}
			if (rootsToPlanIds.size()<=0) {
				throw new RuntimeException("No root element found");
			}
			
			for (String rootId : rootsToPlanIds.keySet()) {
				AbstractArtefact root = artefacts.get(rootId);
				Plan plan = new Plan();
				plan.setId(rootsToPlanIds.get(rootId));
				plan.setAttributes(root.getAttributes());
				addChilds(root,artefacts,artefactsChilds,rootsToPlanIds);
				plan.setRoot(root);
				plan.setVisible(true);
				objectEnricher.accept(plan);
				plan = planAccessor.save(plan);
			}
			
		}
	}


	private void addChilds(AbstractArtefact artefact, Map<String, AbstractArtefact> artefacts,
			Map<String, String[]> artefactsChilds, Map<String,ObjectId> rootsToPlanIds) {
		if (artefactsChilds.containsKey(artefact.getId().toString())) {
			for (String key: artefactsChilds.get(artefact.getId().toString())) {
				AbstractArtefact child = artefacts.get(key);
				artefact.addChild(child);
				addChilds(child,artefacts,artefactsChilds,rootsToPlanIds);
			}
		}
	
	}

}
