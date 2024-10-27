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
package step.reporting;

import step.artefacts.TestSet;
import step.artefacts.reports.CustomReportType;
import step.core.artefacts.reports.*;
import step.core.artefacts.reports.ReportTreeVisitor.ReportNodeEvent;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@link ReportWriter} that generates JUnit 4 XML reports based on the JUnit schema https://github.com/windyroad/JUnit-Schema/blob/master/JUnit.xsd
 *
 */
public class JUnit4ReportWriter implements ReportWriter {

	public JUnit4ReportWriter() {
	}

	@Override
	public void writeReport(ReportTreeAccessor reportTreeAccessor, String executionId, File outputFile) throws IOException {

		try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
			writeReport(reportTreeAccessor, executionId, writer);
		}
	}

	/**
	 * @return the file name of report
	 */
	public String writeReport(ReportTreeAccessor reportTreeAccessor, String executionId, Writer writer) throws IOException {
		ReportTreeVisitor visitor = new ReportTreeVisitor(reportTreeAccessor);
		// Using AtomicInteger and StringBuilder because of the "final limitation" in lambdas...
		AtomicInteger numberOfTests = new AtomicInteger(0);
		AtomicInteger numberOfFailures = new AtomicInteger(0);
		AtomicInteger numberOfErrors = new AtomicInteger(0);
		AtomicInteger numberOfSkipped = new AtomicInteger(0);
		AtomicLong duration = new AtomicLong();
		AtomicLong executionTime = new AtomicLong();
		StringBuilder name = new StringBuilder();

		// First visit the report tree to get the root node informations and the different counts
		visitor.visit(executionId, e->{
			if(e.getStack().isEmpty()) {
				name.append(e.getNode().getName());
				duration.set(e.getNode().getDuration());
				executionTime.set(e.getNode().getExecutionTime());

				// for not test set we only take the root element into account
				if (!isTestSet(e.getNode())) {
					calculateNode(e, numberOfTests, numberOfFailures, numberOfErrors, numberOfSkipped);
				}

			}

			// for test set we count test cases on level 1
			if (e.getStack().size() == 1) {
				if (isTestSet(e.getStack().firstElement())) {
					calculateNode(e, numberOfTests, numberOfFailures, numberOfErrors, numberOfSkipped);
				}
			}
		});

		writer.write("<testsuite name=\""+name.toString()+"\" " +
				"time=\""+ formatDuration(getTestSuiteDuration(duration))+"\" " +
				"timestamp=\""+ formatTimestamp(getExecutionTime(executionTime))+"\" " +
				"hostname=\""+ getHostName() +"\" " +
				"tests=\""+numberOfTests.get()+"\" " +
				"skipped=\""+numberOfSkipped.get()+"\" " +
				"failures=\""+numberOfFailures.get()+"\" " +
				"errors=\""+numberOfErrors.get()+"\">");
		writer.write('\n');
		writer.write("<properties></properties>");
		writer.write('\n');

		AtomicBoolean errorWritten = new AtomicBoolean(false);
		// visit the tree again and write the <testcase> blocks
		visitor.visit(executionId, new ReportNodeVisitorEventHandler() {
			@Override
			public void startReportNode(ReportNodeEvent event) {
				ReportNode node = event.getNode();
				try {
					// for test sets we take test cases from the first level
					// for other root nodes we take the top level only
					if(event.getStack().isEmpty()){
						if(!isTestSet(event.getNode())){
							writeTestCaseBegin(node);
						}
					}

					// as a convention report the children of the first level as testcases
					if(event.getStack().size()==1) {
						if(!skipReportNode(event)) {
							writeTestCaseBegin(node);
						}
					} else if (event.getStack().size()>1) {
						// report all the errors of the sub nodes (level > 1)
						if(node.getError() != null && !errorWritten.get()) {
							writeErrorOrFailure(writer, node, errorWritten);
						}
					}
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
			}

			private void writeTestCaseBegin(ReportNode node) throws IOException {
				writer.write("<testcase classname=\""+ node.getClass().getName()+"\" " +
						"name=\""+ node.getName()+"\" " +
						"time=\""+ formatDuration(getTestCaseDuration(node))+"\">");
				writer.write('\n');
				errorWritten.set(false);
			}

			private void writeTestCaseEnd() throws IOException {
				writer.write("</testcase>");
				writer.write('\n');
			}

			@Override
			public void endReportNode(ReportNodeEvent event) {
				if (event.getStack().isEmpty()) {
					if (!isTestSet(event.getNode())) {
						try {
							writeTestCaseEnd();
						} catch (IOException e1) {
							throw new RuntimeException(e1);
						}
					}
				}

				if(event.getStack().size()==1) {
					if(!skipReportNode(event)) {
						ReportNode node = event.getNode();
						try {
							// if no error has been found in the sub-nodes, report the error for this node
							if(node.getStatus()!=ReportNodeStatus.PASSED && !errorWritten.get()) {
								writeErrorOrFailure(writer, node, errorWritten);
							}
							// close the <testcase> block
							writeTestCaseEnd();
						} catch (IOException e1) {
							throw new RuntimeException(e1);
						}
					}
				}
			}

			protected boolean skipReportNode(ReportNodeEvent nodeEvent) {
				if (nodeEvent.getNode().getStatus() == ReportNodeStatus.SKIPPED || nodeEvent.getNode().getStatus() == ReportNodeStatus.NORUN) {
					return true;
				}

				Stack<ReportNode> stack = nodeEvent.getStack();
				return !stack.isEmpty() && !(isTestSet(stack.firstElement()));
            }
		});

		writer.write("<system-out></system-out>");
		writer.write('\n');
		writer.write("<system-err></system-err>");
		writer.write('\n');
		writer.write("</testsuite>");
		writer.write('\n');

		writer.flush();

		return prepareReportFileName(name.toString(), executionId);
	}

	protected Integer getTestCaseDuration(ReportNode node) {
		return node.getDuration();
	}

	protected long getTestSuiteDuration(AtomicLong duration) {
		return duration.get();
	}

	protected long getExecutionTime(AtomicLong executionTime) {
		return executionTime.get();
	}

	protected String getHostName() throws UnknownHostException {
		return InetAddress.getLocalHost().getHostName();
	}

	private String prepareReportFileName(String name, String executionId) {
		// use timestamp instead of plan name, because plan name can contain forbidden characters for file name
		String formattedTimestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSSSSS").format(LocalDateTime.now()) ;
		String fileName = formattedTimestamp + "-" + CustomReportType.JUNIT.name().toLowerCase();
		return fileName + ".xml";
	}

	private static boolean isTestSet(ReportNode node) {
		return node.getResolvedArtefact() instanceof TestSet;
	}

	private static void calculateNode(ReportNodeEvent e, AtomicInteger numberOfTests, AtomicInteger numberOfFailures, AtomicInteger numberOfErrors, AtomicInteger numberOfSkipped) {
		numberOfTests.incrementAndGet();
		ReportNode node = e.getNode();
		if(node.getStatus() == ReportNodeStatus.FAILED) {
			numberOfFailures.incrementAndGet();
		} else if(node.getStatus() == ReportNodeStatus.TECHNICAL_ERROR) {
			numberOfErrors.incrementAndGet();
		} else if(node.getStatus() == ReportNodeStatus.SKIPPED || node.getStatus() == ReportNodeStatus.NORUN) {
			numberOfSkipped.incrementAndGet();
		}
	}

	protected void writeErrorOrFailure(Writer writer, ReportNode node, AtomicBoolean errorWritten) throws IOException {
		String errorMessage = "";
		if(node.getError()!=null && node.getError().getMsg()!=null) {
			errorMessage = node.getError().getMsg();
		}

		if(node.getStatus()!=ReportNodeStatus.PASSED) {
			if(node.getStatus() == ReportNodeStatus.FAILED) {
				writer.write("<failure type=\"\" message=\"" + errorMessage + "\"/>");
				writer.write('\n');
				errorWritten.set(true);
			} else if(node.getStatus() == ReportNodeStatus.TECHNICAL_ERROR) {
				writer.write("<error type=\"\">"+errorMessage+"</error>");
				writer.write('\n');
				errorWritten.set(true);
			} else {
				writer.write("<error type=\"\">No error message was reported but the status of the report node was "+node.getStatus().toString()+"</error>");
				writer.write('\n');
				errorWritten.set(true);
			}
			
			node.getAttachments().forEach(attachment->{
				
			});
		}
	}

	protected String formatDuration(long duration) {
		// set the english local explicitly to set fixed decimal separators (dots) and make the report not locale-specific
		DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.ENGLISH));
		return df.format(duration/1000.0);
	}

	protected String formatTimestamp(long timeMillis) {
		return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").format(LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), ZoneId.systemDefault()));
	}

}
