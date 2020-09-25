/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.plans.runner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.artefacts.reports.ReportTreeAccessor;
import step.core.artefacts.reports.ReportTreeVisitor;
import step.core.artefacts.reports.ReportTreeVisitor.ReportNodeEvent;
import step.reporting.ReportWriter;

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
	
	public ReportNodeStatus getResult() {
		ReportNode rootReportNode = reportTreeAccessor.get(rootReportNodeId);
		if(rootReportNode != null) {
			ReportNodeStatus status = rootReportNode.getStatus();
			return status;
		} else {
			return ReportNodeStatus.NORUN;
		}
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
		ReportTreeVisitor visitor = getReportTreeVisitor();
		visitor.visitNodes(rootReportNodeId, consumer);
		return this;
	}
	
	/**
	 * Visits the report tree of the execution using the {@link Consumer} of {@link ReportNodeEvent}
	 * @param consumer 
	 * @return
	 */
	public PlanRunnerResult visitReportTree(Consumer<ReportNodeEvent> consumer) {
		ReportTreeVisitor visitor = getReportTreeVisitor();
		visitor.visit(rootReportNodeId, consumer);
		return this;
	}

	protected ReportTreeVisitor getReportTreeVisitor() {
		return new ReportTreeVisitor(reportTreeAccessor);
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
	
	public String getTreeAsString() throws IOException {
		StringWriter stringWriter = new StringWriter();
		printTree(stringWriter);
		return stringWriter.toString();
	}
	
	/**
	 * Writes a report of the execution using the provided {@link ReportWriter} 
	 * 
	 * @param reportWriter the {@link ReportWriter} to be used to write the report
	 * @param outputFile the output file of the report
	 * @return this instance
	 * @throws IOException if an exception occurs while writing the report
	 */
	public PlanRunnerResult writeReport(ReportWriter reportWriter, File outputFile) throws IOException {
		reportWriter.writeReport(reportTreeAccessor, executionId, outputFile);		
		return this;
	}
}
