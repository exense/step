package step.core.artefacts.reports;

import java.util.Iterator;

public interface ReportTreeAccessor {

	public ReportNode get(String id);
	
	public Iterator<ReportNode> getChildren(String parentID);
}
