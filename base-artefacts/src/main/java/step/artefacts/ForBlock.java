package step.artefacts;

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

	public Integer getStart() {
		return start!=null&&start.length()>0?Integer.parseInt(start):0;
	}

	public Integer getEnd() {
		return end!=null&&end.length()>0?Integer.parseInt(end):null;
	}

	public Integer getInc() {
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
}
