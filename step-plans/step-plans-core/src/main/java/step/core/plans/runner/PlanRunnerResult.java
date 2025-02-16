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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.AttachmentMeta;
import step.core.artefacts.reports.*;
import step.core.artefacts.reports.ReportTreeVisitor.ReportNodeEvent;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionProvider;
import step.core.reports.Error;
import step.reporting.ReportWriter;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionContent;

/**
 * This class provides an API for the manipulation of plan executions
 *
 */
public class PlanRunnerResult {

	private static final Logger logger = LoggerFactory.getLogger(PlanRunnerResult.class);

	protected final String executionId;

	protected final ExecutionProvider executionProvider;
	protected final ReportTreeAccessor reportTreeAccessor;
	protected final ResourceManager resourceManager;

	public PlanRunnerResult(String executionId, ExecutionProvider executionProvider, ReportTreeAccessor reportTreeAccessor) {
		this(executionId, executionProvider, reportTreeAccessor, null);
	}
	
	public PlanRunnerResult(String executionId, ExecutionProvider executionProvider, ReportTreeAccessor reportTreeAccessor, ResourceManager resourceManager) {
		super();
		this.executionId = executionId;
		this.executionProvider = executionProvider;
		this.reportTreeAccessor = reportTreeAccessor;
		this.resourceManager = resourceManager;
	}
	
	public ReportNodeStatus getResult() {
		Execution execution = executionProvider.get(executionId);
		return execution.getResult();
	}

	protected List<Error> getLifecycleErrors() {
		Execution execution = executionProvider.get(executionId);
		return execution.getLifecycleErrors();
	}

	public ReportNode getRootReportNode() {
		return reportTreeAccessor.get(executionId);
	}

	/**
	 * @return a stream of all report nodes with errors that caused this execution to fail.
	 * This returns only the so-called contributing errors according to following definition {@link ReportNode#getContributingError()}
	 */
	public Stream<ReportNode> getReportNodesWithErrors() {
		return getReportTreeAccessor().getReportNodesWithContributingErrors(executionId);
	}

	/**
	 * @return a stream of all errors that caused this execution to fail.
	 * This returns only the so-called contributing errors according to following definition {@link ReportNode#getContributingError()}
	 */
	public Stream<Error> getErrors() {
		List<Error> lifecycleErrors = getLifecycleErrors();
		Stream<Error> reportNodeErrors = getReportNodesWithErrors().map(ReportNode::getError).filter(Objects::nonNull);
		return Stream.concat(Optional.ofNullable(lifecycleErrors).orElse(List.of()).stream(), reportNodeErrors);
	}

	/**
	 * @return a concatenation of the first 10 error messages that caused this execution to fail.
	 * This considers only the so-called contributing errors according to following definition {@link ReportNode#getContributingError()}
	 */
	public String getErrorSummary() {
		return getErrors().map(Error::getMsg).limit(10).collect(Collectors.joining(";"));
	}

	/**
	 * @return a {@link Set} containing the error codes of the first n errors where n is set by the limit parameter.
	 * @param limit the max errors to be scanned
	 */
	public Set<Integer> getErrorCodes(int limit) {
		return getErrors().map(Error::getCode).limit(limit).collect(Collectors.toSet());
	}

	public String getExecutionId() {
		return executionId;
	}

	public ReportTreeAccessor getReportTreeAccessor() {
		return reportTreeAccessor;
	}
	
	/**
	 * Visits the report tree of the execution using the {@link Consumer} of {@link ReportNode}
	 * @param consumer the visitor
	 * @return this instance
	 */
	public PlanRunnerResult visitReportNodes(Consumer<ReportNode> consumer) {
		ReportTreeVisitor visitor = getReportTreeVisitor();
		visitor.visitNodes(executionId, consumer);
		return this;
	}
	
	/**
	 * Visits the report tree of the execution using the {@link Consumer} of {@link ReportNodeEvent}
	 * @param consumer the visitor
	 * @return this instance
	 */
	public PlanRunnerResult visitReportTree(Consumer<ReportNodeEvent> consumer) {
		ReportTreeVisitor visitor = getReportTreeVisitor();
		visitor.visit(executionId, consumer);
		return this;
	}

	protected ReportTreeVisitor getReportTreeVisitor() {
		return new ReportTreeVisitor(reportTreeAccessor);
	}
	
	/**
	 * Wait for an the execution to terminate
	 * @param timeout the timeout in ms
	 * @return this instance
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public PlanRunnerResult waitForExecutionToTerminate(long timeout) throws TimeoutException, InterruptedException {
		return this;
	}
	
	/**
	 * Wait indefinitely for an the execution to terminate
	 * @return this instance
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public PlanRunnerResult waitForExecutionToTerminate() throws TimeoutException, InterruptedException {
		return waitForExecutionToTerminate(0);
	}
	
	/**
	 * Prints the result tree to the standard output
	 * @return this instance
	 * @throws IOException
	 */
	public PlanRunnerResult printTree() throws IOException {
		return printTree(new OutputStreamWriter(System.out), true, false);
	}
	
	/**
	 * Prints the result tree to the {@link Writer} provided as input
	 * @param writer 
	 * @return this instance
	 * @throws IOException
	 */
	public PlanRunnerResult printTree(Writer writer) throws IOException {
		return printTree(writer, false);
	}

	/**
	 * Prints the result tree to the {@link Writer} provided as input
	 *
	 * @param writer
	 * @param printAttachments if the attachments of the report nodes have to be
	 *                         printed out (currently restricted to attachments
	 *                         called "exception.log")
	 * @return this instance
	 * @throws IOException
	 */
	public PlanRunnerResult printTree(Writer writer, boolean printAttachments) throws IOException {
		return printTree(writer, false, printAttachments);
	}

	/**
	 * Prints the result tree to the {@link Writer} provided as input
	 * 
	 * @param writer
	 * @param printNodeDetails if the details of the report nodes should be printed out
	 * @param printAttachments if the attachments of the report nodes have to be
	 *                         printed out (currently restricted to attachments
	 *                         called "exception.log")
	 * @return this instance
	 * @throws IOException
	 */
	public PlanRunnerResult printTree(Writer writer, boolean printNodeDetails, boolean printAttachments) throws IOException {
		BufferedWriter bWriter = new BufferedWriter(writer);
		AtomicReference<ParentSource> previousParentSource = new AtomicReference<>(ParentSource.MAIN);
		AtomicInteger previousStackSize = new AtomicInteger(0);
		visitReportTree(event->{
			try {
				ReportNode node = event.getNode();
				int stackSize = event.getStack().size();
				//if custom parent source, print the name of the source once, all children are then printed with one more indentation
				boolean customParentSource = node.getParentSource().printSource;
				boolean newContext = (!node.getParentSource().equals(previousParentSource.get()) || stackSize != previousStackSize.get());
				previousParentSource.set(node.getParentSource());
				previousStackSize.set(stackSize);
				if (customParentSource && newContext) {
					indent(stackSize, bWriter);
					bWriter.write("[");
					bWriter.write(node.getParentSource().name());
					bWriter.write("]\n");
				}
				//indent row, rows with custom parent source have one more level
				int indentSize = (customParentSource) ? stackSize + 1 : stackSize;
				indent(indentSize, bWriter);
				StringBuilder row = new StringBuilder();
				row.append(node.getName());
				String reportAsString = node.getReportAsString();
				if (printNodeDetails && reportAsString != null) {
					row.append("(").append(reportAsString).append(")");
				}
				if (node.getStatus() != null) {
					row.append(":").append(node.getStatus()).append(":");
				}
				if (node.getError() != null) {
					row.append(node.getError().getMsg());
				}
				bWriter.write(row.toString());
				bWriter.write("\n");
				
				if(printAttachments) {
					List<AttachmentMeta> attachments = node.getAttachments();
					if(attachments != null) {
						attachments.forEach(a->{
							Resource resource = resourceManager.getResource(a.getId().toString());
							// TODO create constant for value "exception.log"
							if(resource.getResourceName().equals("exception.log")) {
								try {
									bWriter.write("Stacktrace: \n");
									ResourceRevisionContent resourceContent = resourceManager.getResourceContent(a.getId().toString());
									try {
										IOUtils.copy(resourceContent.getResourceStream(), bWriter, StandardCharsets.UTF_8);
									} finally {
										resourceContent.close();
									}
								} catch (IOException e) {
									logger.error("Error while writing attachment", e);
								}
							}
						});
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("Error while printing tree",e);
			}
		});
		bWriter.flush();
		return this;
	}

	private static void indent(int indentsize, BufferedWriter bWriter) throws IOException {
		for(int i = 0; i< indentsize; i++) {
				bWriter.write(" ");
		}
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
