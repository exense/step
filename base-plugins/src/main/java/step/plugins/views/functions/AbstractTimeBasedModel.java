package step.plugins.views.functions;

import java.util.TreeMap;

import step.plugins.views.ViewModel;

public class AbstractTimeBasedModel<T> extends ViewModel {
	
	private int resolutionIndex = 0;
	
	private int resolution;
	
	private TreeMap<Long, T> intervals;

	private long minTime=Long.MAX_VALUE;
	
	private long maxTime=0;

	public AbstractTimeBasedModel() {
		super();
	}

	public int getResolutionIndex() {
		return resolutionIndex;
	}

	public void setResolutionIndex(int resolutionIndex) {
		this.resolutionIndex = resolutionIndex;
	}

	public TreeMap<Long, T> getIntervals() {
		return intervals;
	}

	public void setIntervals(TreeMap<Long, T> intervals) {
		this.intervals = intervals;
	}

	public int getResolution() {
		return resolution;
	}

	public void setResolution(int resolution) {
		this.resolution = resolution;
	}

	public long getMinTime() {
		return minTime;
	}

	public void setMinTime(long minTime) {
		this.minTime = minTime;
	}

	public long getMaxTime() {
		return maxTime;
	}

	public void setMaxTime(long maxTime) {
		this.maxTime = maxTime;
	}
}
