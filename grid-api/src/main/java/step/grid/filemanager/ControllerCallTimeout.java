package step.grid.filemanager;

@SuppressWarnings("serial")
public class ControllerCallTimeout extends Exception {

	private final long timeout;

	public ControllerCallTimeout(Throwable cause, long timeout) {
		super(cause);
		this.timeout = timeout;
	}

	public long getTimeout() {
		return timeout;
	}
}
