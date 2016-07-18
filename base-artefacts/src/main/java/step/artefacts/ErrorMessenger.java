package step.artefacts;

import java.util.List;

import step.artefacts.handlers.ErrorMessengerHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;

@Artefact(name = "ErrorMessenger", handler = ErrorMessengerHandler.class)
public class ErrorMessenger extends AbstractArtefact {
		
	private List<String> errorMessages;

	public List<String> getErrorMessages() {
		return errorMessages;
	}

	public void setErrorMessages(List<String> errorMessages) {
		this.errorMessages = errorMessages;
	}
}
