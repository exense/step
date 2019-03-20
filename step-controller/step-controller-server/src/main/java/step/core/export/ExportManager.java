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

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.deployment.JacksonMapperProvider;

public class ExportManager {
	
	private static final Logger logger = LoggerFactory.getLogger(ExportManager.class);
	
	ArtefactAccessor accessor;	
	
	public ExportManager(ArtefactAccessor accessor) {
		super();
		this.accessor = accessor;
	}

	public void exportArtefactWithChildren(String artefactId, OutputStream outputStream) throws FileNotFoundException, IOException {
		ObjectMapper mapper = getMapper();	
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
			exportArtefactRecursive(mapper, writer, new ObjectId(artefactId));
		} catch(Exception e) {
			logger.error("Error while exporting artefact with id "+artefactId,e);
		}
	}

	private ObjectMapper getMapper() {
		ObjectMapper mapper = JacksonMapperProvider.createMapper();
		mapper.getFactory().disable(Feature.AUTO_CLOSE_TARGET);
		return mapper;
	}
	
	public void exportAllArtefacts(OutputStream outputStream) throws FileNotFoundException, IOException {
		ObjectMapper mapper = getMapper();
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
			accessor.getRootArtefacts().forEachRemaining((a)->{
				try {
					exportArtefactRecursive(mapper, writer, new ObjectId(a.getId().toString()));
				} catch (Exception e) {
					logger.error("Error while exporting artfact "+a.getId().toString(), e);
				}
			});
		}	

	}

	private void exportArtefactRecursive(ObjectMapper mapper, Writer writer, ObjectId id) throws IOException {
		AbstractArtefact artefact = accessor.get(id);
		mapper.writeValue(writer, artefact);
		writer.write("\n");
		if(artefact.getChildrenIDs()!=null) {
			for(ObjectId childId:artefact.getChildrenIDs()) {
				exportArtefactRecursive(mapper, writer, childId);
			}
		}
	}
}
