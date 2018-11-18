/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.core.reports;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class MeasurementsBuilder {

	private Stack<Measure> stack = new Stack<Measure>();
		
	private List<Measure> closedMeasures = new ArrayList<>();

	public void startMeasure(String name) {
		pushMeasure(new Measure(name, 0, System.currentTimeMillis(), null));
	}
	
	public void startMeasure(String name, long begin) {
		pushMeasure(new Measure(name, 0, begin, null));
	}
	
	protected void pushMeasure(Measure tr) {
		synchronized (stack) {
			stack.push(tr);			
		}
	}
	
	private Measure stopMeasure(long end, Map<String, Object> data) {
		Measure tr = null;
		synchronized (stack) {
			tr = stack.pop();
		}
		
		if(tr!=null) {
			tr.setDuration(end-tr.getBegin());
			tr.setData(data);
			synchronized (closedMeasures) {
				closedMeasures.add(tr);
			}
		} else {
			throw new RuntimeException("No measure has been started. Please ensure to first call startMeasure before calling stopMeasure.");
		}
		
		return tr;
	}
	
	public Measure stopMeasure(Map<String, Object> data) {
		return stopMeasure(System.currentTimeMillis(), data);
	}
	
	public Measure stopMeasure() {
		return stopMeasure(null);
	}
	
	public void addMeasure(String measureName, long aDurationMillis) {
		addMeasure(measureName, aDurationMillis, null);
	}
	
	public void addMeasure(String measureName, long aDurationMillis, Map<String, Object> data) {
		synchronized (closedMeasures) {
			closedMeasures.add(new Measure(measureName, aDurationMillis, System.currentTimeMillis(), data));
		}
	}
	
	public void addMeasure(Measure measure) {
		synchronized (closedMeasures) {
			closedMeasures.add(measure);
		}
	}
	
	public void addMeasures(List<Measure> measures) {
		synchronized (closedMeasures) {
			closedMeasures.addAll(measures);
		}
	}
	
	public List<Measure> getMeasures() {
		synchronized (closedMeasures) {
			return new ArrayList<Measure>(closedMeasures);			
		}
	}
}
