package step.handlers.javahandler;

import step.grid.io.OutputMessage;

@SuppressWarnings("serial")
public class KeywordException extends Exception {

	private final OutputMessage outputMessage;
	
	public KeywordException(OutputMessage outputMessage, Throwable cause) {
		super(cause);
		this.outputMessage = outputMessage;
	}

	public KeywordException(OutputMessage outputMessage, String message, Throwable cause) {
		super(message, cause);
		this.outputMessage = outputMessage;
	}

	public KeywordException(OutputMessage outputMessage, String message) {
		super(message);
		this.outputMessage = outputMessage;
	}

	public OutputMessage getOutputMessage() {
		return outputMessage;
	}

}
