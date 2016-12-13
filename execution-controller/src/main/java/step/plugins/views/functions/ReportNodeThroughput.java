package step.plugins.views.functions;

import java.util.Map;

import step.plugins.views.ViewModel;

public class ReportNodeThroughput extends ViewModel {
	
	Map<Long, ReportNodeThroughputEntry> intervals;

	public ReportNodeThroughput() {
		super();
	}

	public Map<Long, ReportNodeThroughputEntry> getIntervals() {
		return intervals;
	}

	public void setIntervals(Map<Long, ReportNodeThroughputEntry> intervals) {
		this.intervals = intervals;
	}

	public static class ReportNodeThroughputEntry {
		
		int count;
	}
}
