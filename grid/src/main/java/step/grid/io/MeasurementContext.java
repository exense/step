package step.grid.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class MeasurementContext {

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
	
	public void stopMeasure(long end, Map<String, String> data) {
		Measure tr;
		synchronized (stack) {
			tr = stack.pop();
		}
		
		if(tr!=null) {
			tr.setDuration(end-tr.getBegin());
			tr.setData(data);
			System.out.println("Closing "+tr.getName());
			synchronized (closedMeasures) {
				closedMeasures.add(tr);
			}
		} else {
			throw new RuntimeException("No measure has been started. Please ensure to first call startMeasure before calling stopMeasure.");
		}		
	}
	
	public void stopMeasure(Map<String, String> data) {
		stopMeasure(System.currentTimeMillis(), data);
	}
	
	public void stopMeasure() {
		stopMeasure(null);
	}
	
	public void addMeasure(String measureName, long aDurationMillis) {
		synchronized (closedMeasures) {
			closedMeasures.add(new Measure(measureName, aDurationMillis, System.currentTimeMillis(), null));
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
