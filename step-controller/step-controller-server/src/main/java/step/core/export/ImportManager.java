package step.core.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.deployment.JacksonMapperProvider;
import step.core.objectenricher.ObjectEnricher;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;

public class ImportManager {
	
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
		}
	}

}
