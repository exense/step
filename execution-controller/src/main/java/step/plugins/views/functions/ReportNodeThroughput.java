package step.plugins.views.functions;

import java.util.TreeMap;

import step.plugins.views.ViewModel;

public class ReportNodeThroughput extends ViewModel {
	
	private long resolution;
	
	private TreeMap<Long, ReportNodeThroughputEntry> intervals;

	public ReportNodeThroughput() {
		super();
	}

	public TreeMap<Long, ReportNodeThroughputEntry> getIntervals() {
		return intervals;
	}

	public void setIntervals(TreeMap<Long, ReportNodeThroughputEntry> intervals) {
		this.intervals = intervals;
	}

	public long getResolution() {
		return resolution;
	}

	public void setResolution(long resolution) {
		this.resolution = resolution;
	}

	public static class ReportNodeThroughputEntry {
		
		public ReportNodeThroughputEntry() {
			super();
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		int count;
	}
}
