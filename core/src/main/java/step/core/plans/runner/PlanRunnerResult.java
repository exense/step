package step.core.plans.runner;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportTreeAccessor;
import step.core.artefacts.reports.ReportTreeVisitor;

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
	
	public PlanRunnerResult visitReportTree(Consumer<ReportNode> consumer) {
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
	
	public void printTree(Writer printer) throws IOException {
		printReportNode(new BufferedWriter(printer), reportTreeAccessor.get(rootReportNodeId), 0);
	}
	
	protected void printReportNode(BufferedWriter printer, ReportNode node, int level) throws IOException {
		StringBuilder builder = new StringBuilder();
		for(int i=0;i<level;i++) {
			builder.append(" ");
		}
		builder.append(node.getName()+":"+node.getStatus()+":"+(node.getError()!=null?node.getError().getMsg():""));
		printer.write(builder.toString());
		printer.write("\n");
		printer.flush();
		Iterator<ReportNode> it = reportTreeAccessor.getChildren(node.getId().toString());
		while(it.hasNext()) {
			printReportNode(printer, it.next(), level+1);
		}
	}
	
	
}
