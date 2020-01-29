package step.core.export;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.deployment.JacksonMapperProvider;
import step.core.objectenricher.ObjectFilter;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;

public class ExportManager {
	
	private static final Logger logger = LoggerFactory.getLogger(ExportManager.class);
	
	PlanAccessor accessor;	
	
	public ExportManager(PlanAccessor accessor) {
		super();
		this.accessor = accessor;
	}

	public void exportPlan(String planId, OutputStream outputStream) throws FileNotFoundException, IOException {
		ObjectMapper mapper = getMapper();	
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
			exportPlan(mapper, writer, new ObjectId(planId));
		} catch(Exception e) {
			logger.error("Error while exporting artefact with id "+planId,e);
		}
	}

	private ObjectMapper getMapper() {
		ObjectMapper mapper = JacksonMapperProvider.createMapper();
		mapper.getFactory().disable(Feature.AUTO_CLOSE_TARGET);
		return mapper;
	}
	
	public void exportAllPlans(OutputStream outputStream, ObjectFilter objectFilter) throws FileNotFoundException, IOException {
		ObjectMapper mapper = getMapper();
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
			accessor.getAll().forEachRemaining((a)->{
				if(objectFilter.test(a)) {
					try {
						exportPlan(mapper, writer, new ObjectId(a.getId().toString()));
					} catch (Exception e) {
						logger.error("Error while exporting artfact "+a.getId().toString(), e);
					}
				}
			});
		}	

	}

	private void exportPlan(ObjectMapper mapper, Writer writer, ObjectId id) throws IOException {
		Plan plan = accessor.get(id);
		mapper.writeValue(writer, plan);
		writer.write("\n");
	}
}
