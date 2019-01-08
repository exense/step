package step.core.reports;

/**
 * This class is used to represent all errors that might occur
 * in STEP either in a control or a function (aka keyword).
 * 
 * It can represent a technical error (i.e. an unhandled or unexpected error) 
 * or a clearly identified error in the SUT
 *
 */
public class Error {
	
	protected ErrorType type = ErrorType.TECHNICAL;
	
	protected String layer;
	
	protected String msg;
	
	protected Integer code;
	
	protected boolean root;

	/**
	 * @param type the type of error
	 * @param message the detailed error message that will be reported 1:1 to the end user)
	 */
	public Error(ErrorType type, String message) {
		this(type, null, message, 0, true);
	}

	/**
	 * @param type the type of error 
	 * @param message the detailed error message (that will be reported 1:1 to the end user)
	 * @param code a free definable error code to uniquely classify the error that can be used to filter errors in reports
	 */
	public Error(ErrorType type, String message, Integer code) {
		this(type, null, message, code, true);
	}

	/**
	 * @param type the type of error 
	 * @param layer a free text field that describes the layer of the application where the error occurred
	 * @param msg the detailed error message (that will be reported 1:1 to the end user)
	 * @param code a free definable error code to uniquely classify the error that can be used to filter errors in reports
	 * @param root a boolean that defines if this is the root cause of an error or a rethrown error (set to true in 
	 */
	public Error(ErrorType type, String layer, String msg, Integer code, boolean root) {
		super();
		this.type = type;
		this.layer = layer;
		this.msg = msg;
		this.code = code;
		this.root = root;
	}

	public Error() {
		super();
	}

	public ErrorType getType() {
		return type;
	}

	public void setType(ErrorType type) {
		this.type = type;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public boolean isRoot() {
		return root;
	}

	public void setRoot(boolean root) {
		this.root = root;
	}

	public String getLayer() {
		return layer;
	}

	public void setLayer(String layer) {
		this.layer = layer;
	}

}
