package step.core.plans.runner;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportTreeAccessor;
import step.core.artefacts.reports.ReportTreeVisitor;
import step.core.artefacts.reports.ReportTreeVisitor.ReportNodeEvent;

/**
 * This class provides an API for the manipulation of plan executions
 *
 */
public class PlanRunnerResult {

	protected final String executionId;
	
	protected final String rootReportNodeId;
	
	protected final ReportTreeAccessor reportTreeAccessor;

	public PlanRunnerResult(String executionId, String rootReportNodeId, ReportTreeAccessor reportTreeAccessor) {
		super();
		this.executionId = executionId;
		this.rootReportNodeId = rootReportNodeId;
		this.reportTreeAccessor = reportTreeAccessor;
	}

	public String getExecutionId() {
		return executionId;
	}

	public ReportTreeAccessor getReportTreeAccessor() {
		return reportTreeAccessor;
	}
	
	/**
	 * Visits the report tree of the execution using the {@link Consumer} of {@link ReportNode}
	 * @param consumer
	 * @return
	 */
	public PlanRunnerResult visitReportNodes(Consumer<ReportNode> consumer) {
		ReportTreeVisitor visitor = new ReportTreeVisitor(reportTreeAccessor);
		visitor.visitNodes(rootReportNodeId, consumer);
		return this;
	}
	
	/**
	 * Visits the report tree of the execution using the {@link Consumer} of {@link ReportNodeEvent}
	 * @param consumer 
	 * @return
	 */
	public PlanRunnerResult visitReportTree(Consumer<ReportNodeEvent> consumer) {
		ReportTreeVisitor visitor = new ReportTreeVisitor(reportTreeAccessor);
		visitor.visit(rootReportNodeId, consumer);
		return this;
	}
	
	/**
	 * Wait for an the execution to terminate
	 * @param timeout the timeout in ms
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public PlanRunnerResult waitForExecutionToTerminate(long timeout) throws TimeoutException, InterruptedException {
		return this;
	}
	
	/**
	 * Wait indefinitely for an the execution to terminate
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public PlanRunnerResult waitForExecutionToTerminate() throws TimeoutException, InterruptedException {
		return waitForExecutionToTerminate(0);
	}
	
	/**
	 * Prints the result tree to the standard output
	 * @return
	 * @throws IOException
	 */
	public PlanRunnerResult printTree() throws IOException {
		return printTree(new OutputStreamWriter(System.out));
	}
	
	/**
	 * Prints the result tree to the {@link Writer} provided as input
	 * @param writer 
	 * @return
	 * @throws IOException
	 */
	public PlanRunnerResult printTree(Writer writer) throws IOException {
		BufferedWriter bWriter = new BufferedWriter(writer);
		visitReportTree(event->{
			try {
				for(int i=0;i<event.getStack().size();i++) {
						bWriter.write(" ");
				}
				ReportNode node = event.getNode();
				bWriter.write(node.getName()+":"+node.getStatus()+":"+(node.getError()!=null?node.getError().getMsg():""));
				bWriter.write("\n");
			} catch (IOException e) {
				throw new RuntimeException("Error while printing tree",e);
			}
		});
		bWriter.flush();
		return this;
	}	
}
