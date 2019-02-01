package step.core.artefacts.reports;

import step.core.artefacts.reports.ReportTreeVisitor.ReportNodeEvent;

/**
 *  A support class for {@link ReportTreeVisitor} which listens for report node events.
 *
 */
public interface ReportNodeVisitorEventHandler {
	
	/**
	 * This method is call by the {@link ReportTreeVisitor} when entering a report node
	 * 
	 * @param reportNodeEvent the {@link ReportNodeEvent} corresponding to the entered node
	 */
	public void startReportNode(ReportNodeEvent reportNodeEvent);
	
	/**
	 * This method is call by the {@link ReportTreeVisitor} when exiting a report node
	 * 
	 * @param reportNodeEvent the {@link ReportNodeEvent} corresponding to the exited node
	 */
	public void endReportNode(ReportNodeEvent reportNodeEvent);

}
