package step.core.artefacts.reports;

import java.util.Iterator;

public interface ReportTreeAccessor {

	public Iterator<ReportNode> getChildren(String parentID);
}
