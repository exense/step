package step.core.export;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;

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

	public void exportArtefactWithChildren(String artefactId, File outputFile) throws FileNotFoundException, IOException {
		ObjectMapper mapper = getMapper();	
		try (Writer writer = Files.newBufferedWriter(outputFile.toPath())) {
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
	
	public void exportAllArtefacts(File outputFile) throws FileNotFoundException, IOException {
		ObjectMapper mapper = getMapper();
		try (Writer writer = Files.newBufferedWriter(outputFile.toPath())) {
			accessor.getRootArtefacts().forEachRemaining((a)->{
				try {
					exportArtefactRecursive(mapper, writer, new ObjectId(a.getId().toString()));
				} catch (IOException e) {
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
