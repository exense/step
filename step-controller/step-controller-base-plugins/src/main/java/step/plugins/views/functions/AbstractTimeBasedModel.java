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
