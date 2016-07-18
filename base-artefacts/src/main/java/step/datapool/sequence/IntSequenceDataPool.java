package step.datapool.sequence;

import step.core.artefacts.DynamicAttribute;

public class IntSequenceDataPool {
	
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
}
