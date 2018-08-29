package step.core.plans.runner;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportTreeAccessor;
import step.core.artefacts.reports.ReportTreeVisitor;
import step.core.artefacts.reports.ReportTreeVisitor.ReportNodeEvent;

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
	
	public PlanRunnerResult visitReportNodes(Consumer<ReportNode> consumer) {
		ReportTreeVisitor visitor = new ReportTreeVisitor(reportTreeAccessor);
		visitor.visitNodes(rootReportNodeId, consumer);
		return this;
	}
	
	public PlanRunnerResult visitReportTree(Consumer<ReportNodeEvent> consumer) {
		ReportTreeVisitor visitor = new ReportTreeVisitor(reportTreeAccessor);
		visitor.visit(rootReportNodeId, consumer);
		return this;
	}
	
	public PlanRunnerResult waitForExecutionToTerminate(long timeout) throws TimeoutException, InterruptedException {
		return this;
	}
	
	public PlanRunnerResult waitForExecutionToTerminate() throws TimeoutException, InterruptedException {
		return waitForExecutionToTerminate(0);
	}
	
	public void printTree(Writer writer) throws IOException {
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
	}	
}
