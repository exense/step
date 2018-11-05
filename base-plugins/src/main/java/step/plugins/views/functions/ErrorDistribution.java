package step.plugins.views.functions;

import java.util.Map;

import step.core.accessors.DottedKeyMap;
import step.plugins.views.ViewModel;

public class ErrorDistribution extends ViewModel {
	
	protected long count = 0;

	protected long errorCount = 0;
	
	protected DottedKeyMap<String, Integer> countByErrorMsg = new DottedKeyMap<>();

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

	public Map<String, Integer> getCountByErrorMsg() {
		return countByErrorMsg;
	}

	public void setCountByErrorMsg(DottedKeyMap<String, Integer> countByErrorMsg) {
		this.countByErrorMsg = countByErrorMsg;
	}
}
