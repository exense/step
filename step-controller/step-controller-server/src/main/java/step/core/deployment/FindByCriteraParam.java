package step.core.deployment;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FindByCriteraParam {
	@JsonCreator
	public FindByCriteraParam(@JsonProperty("criteria") HashMap<String, String> criteria,
			@JsonProperty("start") Date start, @JsonProperty("end") Date end, 
			@JsonProperty("limit") int limit, @JsonProperty("skip") int skip ) throws Exception 
	{
		super();
		System.out.println("-------------------------------------------------------- "+criteria);
		this.criteria = criteria;
		this.start = start;
		this.end = end;
		this.limit = limit;
		this.skip = skip;
	}
	public Map<String, String> getCriteria() {
		return criteria;
	}
	public void setCriteria(HashMap<String, String> criteria) {
		this.criteria = criteria;
	}
	public Date getStart() {
		return start;
	}
	public void setStart(Date start) {
		this.start = start;
	}
	public Date getEnd() {
		return end;
	}
	public void setEnd(Date end) {
		this.end = end;
	}
	public int getSkip() {
		return skip;
	}
	public void setSkip(int skip) {
		this.skip = skip;
	}
	public int getLimit() {
		return limit;
	}
	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	private HashMap<String, String> criteria = new HashMap<>();
	private Date start;
	private Date end;
	private int skip;
	private int limit;
}