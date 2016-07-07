package step.grid.io;

import java.util.Map;


public class Measure {
	
	private String name;
	
	private long duration;
	
	private long begin;
	
	private Map<String, String> data;

	public Measure() {
		super();
	}

	public Measure(String name, long duration, long begin, Map<String, String> data) {
		super();
		this.name = name;
		this.duration = duration;
		this.begin = begin;
		this.data = data;
	}
	
	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public long getBegin() {
		return begin;
	}

	public void setBegin(long begin) {
		this.begin = begin;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getData() {
		return data;
	}

	public void setData(Map<String, String> data) {
		this.data = data;
	}
}
