package step.plugins.views.functions;

import java.util.HashMap;
import java.util.Map;

public class ErrorRateEntry {

	protected int count;
	
	protected Map<String, Integer> countByErrorMsg = new HashMap<>();
	
	public ErrorRateEntry() {
		super();
	}

	public int getCount() {
		return count;
	}

	public Map<String, Integer> getCountByErrorMsg() {
		return countByErrorMsg;
	}

}
