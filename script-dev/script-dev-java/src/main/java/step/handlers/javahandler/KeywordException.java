package step.handlers.javahandler;

import step.functions.io.Output;

@SuppressWarnings("serial")
public class KeywordException extends Exception {

	private final Output<?> output;
	
	public KeywordException(Output<?> output, Throwable cause) {
		super(getMessage(output), cause);
		this.output = output;
	}

	public KeywordException(Output<?> output) {
		super(getMessage(output));
		this.output = output;
	}
	
	private static String getMessage(Output<?> output) {
		return output.getError()!=null?output.getError().getMsg():"Undefined keywor error message";
	}

	public Output<?> getOutput() {
		return output;
	}

}
