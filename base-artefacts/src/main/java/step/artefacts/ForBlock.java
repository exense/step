package step.artefacts;

import com.fasterxml.jackson.annotation.JsonIgnore;

import step.artefacts.handlers.ForBlockHandler;
import step.core.artefacts.Artefact;
import step.core.artefacts.DynamicAttribute;

@Artefact(name = "For", handler = ForBlockHandler.class)
public class ForBlock extends AbstractForBlock {
		
	@DynamicAttribute
	String start;
	
	@DynamicAttribute
	String end;
	
	@DynamicAttribute
	String inc;

	@JsonIgnore
	public Integer getStartInt() {
		return start!=null&&start.length()>0?Integer.parseInt(start):0;
	}

	@JsonIgnore
	public Integer getEndInt() {
		return end!=null&&end.length()>0?Integer.parseInt(end):null;
	}

	@JsonIgnore
	public Integer getIncInt() {
		return inc!=null&&inc.length()>0?Integer.parseInt(inc):1;
	}

	public void setStart(String start) {
		this.start = start;
	}

	public void setEnd(String end) {
		this.end = end;
	}

	public void setInc(String inc) {
		this.inc = inc;
	}

	public String getStart() {
		return start;
	}

	public String getEnd() {
		return end;
	}

	public String getInc() {
		return inc;
	}
}
