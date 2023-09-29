/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.plugins.views.functions;

import java.util.Map.Entry;
import java.util.TreeMap;

import step.core.views.AbstractView;

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
	
	protected void removePoint(AbstractTimeBasedModel<T> model, long time, T point) {
		//increaseResolutionIfNeeded(model);
		removePointFromInterval(model.getIntervals(), time, model.getResolution(), point);
		//updateMinAndMaxTime(model, time);
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
			mergePointToInterval(newIntervals, time, resolution, entry.getValue());
		}
		
		model.setResolutionIndex(resolutionIndex);
		model.setResolution(resolution);
		model.setIntervals(newIntervals);
	}

	protected void mergePointToInterval(TreeMap<Long, T> newIntervals, long time, int resolution, T point) {
		addPointToInterval(newIntervals, time, resolution, point);
	}
	
	private void addPointToInterval(TreeMap<Long, T> intervals, long time, int resolution, T point) {
		long interval = timeToInterval(time,resolution);

		initInterval(intervals,interval);
		T entry = intervals.get(interval);
		if(entry==null) {
			entry = point;
			intervals.put(interval, entry);
		} else {
			mergePoints(entry, point);
		}
	}
	
	private void removePointFromInterval(TreeMap<Long, T> intervals, long time, int resolution, T point) {
		long interval = timeToInterval(time,resolution);
		initInterval(intervals,interval);
		T entry = intervals.get(interval);
		if(entry != null) {
			unMergePoints(entry, point);
		}
	}

	protected void initInterval(TreeMap<Long, T> intervals,long interval) {}
	
	protected abstract void mergePoints(T target, T source);
	
	protected abstract void unMergePoints(T target, T source);
	
	protected long timeToInterval(long time, long resolution) {
		return time-time%resolution;
	}

}
