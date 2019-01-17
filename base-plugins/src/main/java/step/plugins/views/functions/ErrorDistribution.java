package step.plugins.views.functions;

import step.core.accessors.collections.ViewCounterMap;
import step.plugins.views.ViewModel;

public class ErrorDistribution extends ViewModel {
	
	protected long count = 0;

	protected long errorCount = 0;
	
	protected ViewCounterMap countByErrorCode;

	protected ViewCounterMap countByErrorMsg;
	
	private int otherThreshhold;
	
	private String defaultKey;
	
	public ErrorDistribution(int customThreshold, String defaultKey){
		this.otherThreshhold = customThreshold;
		this.defaultKey = defaultKey;
		init();
	}

	private ErrorDistribution() {
	}
	
	private void init(){
		this.countByErrorCode = new ViewCounterMap(this.otherThreshhold, this.defaultKey);
		this.countByErrorMsg = new ViewCounterMap(this.otherThreshhold, this.defaultKey);
	}
	

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public long getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(long errorCount) {
		this.errorCount = errorCount;
	}

	public void incrementByMsg(String message){
		this.countByErrorMsg.incrementForKey(message);
	}

	public void incrementByCode(String code){
		this.countByErrorCode.incrementForKey(code);
	}

	public ViewCounterMap getCountByErrorCode() {
		return countByErrorCode;
	}

	public void setCountByErrorCode(ViewCounterMap countByErrorCode) {
		this.countByErrorCode = countByErrorCode;
	}

	public ViewCounterMap getCountByErrorMsg() {
		return countByErrorMsg;
	}

	public void setCountByErrorMsg(ViewCounterMap countByErrorMsg) {
		this.countByErrorMsg = countByErrorMsg;
	}
}
