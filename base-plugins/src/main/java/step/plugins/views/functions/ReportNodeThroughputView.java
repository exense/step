package step.plugins.views.functions;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.reports.ReportNode;
import step.plugins.views.View;
import step.plugins.views.functions.ReportNodeThroughput.ReportNodeThroughputEntry;

public class ReportNodeThroughputView extends View<ReportNodeThroughput> {
	
	int currentResolutionIndex = 0;
	int resolutions[] = new int[]{5000,60000,3600000,86400000};
	
	int resolution = resolutions[currentResolutionIndex];

	int threshold = 20;
	
	@Override
	public ReportNodeThroughput init() {
		ReportNodeThroughput model = new ReportNodeThroughput();
		model.setIntervals(new TreeMap<>());
		model.setResolution(resolution);
		return model;
	}

	public int[] getResolutions() {
		return resolutions;
	}

	public void setResolutions(int[] resolutions) {
		this.resolutions = resolutions;
		this.resolution = resolutions[0];
	}

	@Override
	public String getViewId() {
		return "ReportNodeThroughput";
	}

	@Override
	public void afterReportNodeSkeletonCreation(ReportNodeThroughput model, ReportNode node) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterReportNodeExecution(ReportNodeThroughput model, ReportNode node) {
		if(node instanceof CallFunctionReportNode) {
			addPointsToInterval(model.getIntervals(), node.getExecutionTime(), 1);
			
			if(model.getIntervals().size()>threshold) {
				decreaseResolution(model);
			}
		}
	}
	
	private void decreaseResolution(ReportNodeThroughput model) {
		currentResolutionIndex = Math.min(currentResolutionIndex+1, resolutions.length-1);
		resolution = resolutions[currentResolutionIndex];
		
		TreeMap<Long, ReportNodeThroughputEntry> newIntervals = new TreeMap<>();
		TreeMap<Long, ReportNodeThroughputEntry> oldIntervals = model.getIntervals();
		for(Entry<Long, ReportNodeThroughputEntry> entry:oldIntervals.entrySet()) {
			long time = entry.getKey();
			addPointsToInterval(newIntervals, time, entry.getValue().getCount());
		}
		
		model.setResolution(resolution);
		model.setIntervals(newIntervals);
	}

	private void addPointsToInterval(Map<Long, ReportNodeThroughputEntry> intervals, long time, int count) {
		long interval = timeToInterval(time,resolution);
		incrementInterval(intervals, interval, count);
	}
	
	private void incrementInterval(Map<Long, ReportNodeThroughputEntry> intervals, long interval, int count) {
		ReportNodeThroughputEntry entry = intervals.get(interval);
		if(entry==null) {
			entry = new ReportNodeThroughputEntry();
			intervals.put(interval, entry);
		}
		entry.count+=count;
	}
	
	private long timeToInterval(long time, long resolution) {
		return time-time%resolution;
	}

}
