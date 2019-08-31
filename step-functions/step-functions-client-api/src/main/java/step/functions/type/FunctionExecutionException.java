package step.functions.type;

import step.core.reports.Error;

public class FunctionExecutionException extends Exception {

	private static final long serialVersionUID = 4129249138327296185L;
	
	protected final Error error;
	
	protected final Exception source; 

	public FunctionExecutionException(Error error, Exception source) {
		super();
		this.error = error;
		this.source = source;
	}

	public Error getError() {
		return error;
	}

	public Exception getSource() {
		return source;
	}
	
}
