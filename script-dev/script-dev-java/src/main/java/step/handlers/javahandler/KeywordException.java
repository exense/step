package step.handlers.javahandler;

import step.functions.Output;

@SuppressWarnings("serial")
public class KeywordException extends Exception {

	private final Output<?> outputMessage;
	
	public KeywordException(Output<?> outputMessage, Throwable cause) {
		super(cause);
		this.outputMessage = outputMessage;
	}

	public KeywordException(Output<?> outputMessage, String message, Throwable cause) {
		super(message, cause);
		this.outputMessage = outputMessage;
	}

	public KeywordException(Output<?> outputMessage, String message) {
		super(message);
		this.outputMessage = outputMessage;
	}

	public Output<?> getOutputMessage() {
		return outputMessage;
	}

}
