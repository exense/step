package step.core.execution.table;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.execution.model.Execution;

public class RootReportNodeProvider {
				
	protected ReportNodeAccessor reportNodeAccessor;
	
	private static final Logger logger = LoggerFactory.getLogger(RootReportNodeProvider.class);

	public RootReportNodeProvider(GlobalContext context) {
		super();
		reportNodeAccessor = context.getReportAccessor();
	}

	public ReportNode getRootReportNode(Execution execution) {
		String eid = execution.getId().toString();
		
		ReportNode rootReportNode = reportNodeAccessor.getRootReportNode(eid);
		if(rootReportNode!=null) {
			Iterator<ReportNode> rootReportNodeChildren = reportNodeAccessor.getChildren(rootReportNode.getId());
			if(rootReportNodeChildren.hasNext()) {
				rootReportNode = rootReportNodeChildren.next();
				if(rootReportNode != null) {
					return rootReportNode;
				} else {
					logger.error("Error while getting root report node for execution. "
							+ "Iterator.next() returned null although Iterator.hasNext() returned true. "
							+ "This should not occur "+eid);
				}
			} else {
				logger.debug("No children found for report node with id "+rootReportNode.getId());
			}
		}
		return null;
	}
}
