package step.artefacts.handlers.teststep.context;

public class Message {
	
	String message;
	
	Throwable cause;

	public Message(String message) {
		super();
		this.message = message;
	}

	public Message(String message, Throwable cause) {
		super();
		this.message = message;
		this.cause = cause;
	}

	public String getMessage() {
		return message;
	}

	public Throwable getCause() {
		return cause;
	}

}
