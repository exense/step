package step.artefacts.handlers.teststep;

import java.util.UUID;

import step.adapters.commons.model.Input;
import step.adapters.commons.model.OutputBuilder;
import step.grid.client.AdapterClient;
import step.grid.client.AdapterSession;
import step.plugins.keywordrepository.Keyword;

public class AdapterClientMock extends AdapterClient {

	public AdapterClientMock() {
		super();
	}

	@Override
	public ProcessInputResponse processInput(AdapterSession adapterSession, Keyword configuration, Input input, UUID permitId) throws Exception {
		ProcessInputResponse r = new ProcessInputResponse();
		
		OutputBuilder b = new OutputBuilder();
		b.createDocument("Result");
		r.setOutput(b.build());
		return r;
	}
	
	@Override
	public ProcessInputResponse processInput(AdapterSession adapterSession, Keyword configuration, Input input) throws Exception {
		return processInput(adapterSession, configuration, input, null);
	}

	@Override
	public void releaseSession(AdapterSession adapterSession) {
	}

	@Override
	public void close() {
	}

}
