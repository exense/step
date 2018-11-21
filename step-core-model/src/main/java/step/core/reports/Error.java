package step.core.reports;

public class Error {
	
	ErrorType type = ErrorType.TECHNICAL;
	
	String layer;
	
	String msg;
	
	Integer code;
	
	boolean root;

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

}
