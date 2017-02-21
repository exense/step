package step.plugins.views.functions;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import step.plugins.views.AbstractView;

public abstract class AbstractTimeBasedView<T> extends AbstractView<AbstractTimeBasedModel<T>> {

	int resolutions[] = new int[]{5000,60000,3600000,86400000};
	
	int threshold = 20;
		
	@Override
	public AbstractTimeBasedModel<T> init() {
		AbstractTimeBasedModel<T> model = new AbstractTimeBasedModel<>();
		model.setIntervals(new TreeMap<>());
		model.setResolution(resolutions[0]);
		model.setResolutionIndex(0);
		return model;
	}
	
	public void setResolutions(int[] resolutions) {
		this.resolutions = resolutions;
	}
	
	protected void addPoint(AbstractTimeBasedModel<T> model, long time, T point) {
		decreaseResolutionIfNeeded(model);
		addPointToInterval(model.getIntervals(), time, model.getResolution(), point);
		updateMinAndMaxTime(model, time);
	}

	private void updateMinAndMaxTime(AbstractTimeBasedModel<T> model, long time) {
		if(model.getMinTime()>time) {
			model.setMinTime(time);
		}
		if(model.getMaxTime()<time) {
			model.setMaxTime(time);
		}
	}

	private void decreaseResolutionIfNeeded(AbstractTimeBasedModel<T> model) {
		if(model.getIntervals().size()>threshold) {
			decreaseResolution(model);
		}
	}
	
	private void decreaseResolution(AbstractTimeBasedModel<T> model) {
		int resolutionIndex = Math.min(model.getResolutionIndex()+1, resolutions.length-1);
		int resolution = resolutions[resolutionIndex];
		
		TreeMap<Long, T> newIntervals = new TreeMap<>();
		TreeMap<Long, T> oldIntervals = model.getIntervals();
		for(Entry<Long, T> entry:oldIntervals.entrySet()) {
			long time = entry.getKey();
			addPointToInterval(newIntervals, time, resolution, entry.getValue());
		}
		
		model.setResolutionIndex(resolutionIndex);
		model.setResolution(resolution);
		model.setIntervals(newIntervals);
	}
	
	private void addPointToInterval(Map<Long, T> intervals, long time, int resolution, T point) {
		long interval = timeToInterval(time,resolution);
		
		T entry = intervals.get(interval);
		if(entry==null) {
			entry = point;
			intervals.put(interval, entry);
		} else {
			mergePoints(entry, point);
		}
	}
	
	protected abstract void mergePoints(T target, T source);
	
	private long timeToInterval(long time, long resolution) {
		return time-time%resolution;
	}

}
