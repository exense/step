package step.core.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.deployment.JacksonMapperProvider;
import step.core.objectenricher.ObjectEnricher;

public class ImportManager {
	
	protected final ArtefactAccessor artefactAccessor;

	public ImportManager(ArtefactAccessor artefactAccessor) {
		super();
		this.artefactAccessor = artefactAccessor;
	}

	public void importArtefacts(File file, ObjectEnricher objectEnricher) throws IOException {
		ObjectMapper mapper = JacksonMapperProvider.createMapper();
		try(BufferedReader reader = Files.newBufferedReader(file.toPath())) {
			String line;
			while((line=reader.readLine())!=null) {
				AbstractArtefact artefact = mapper.readValue(line, AbstractArtefact.class);
				objectEnricher.accept(artefact);
				artefactAccessor.save(artefact);
			}			
		}
	}

}
