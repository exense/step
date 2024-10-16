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

import java.io.*;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.artefacts.reports.ReportNodeVisitorEventHandler;
import step.core.artefacts.reports.ReportTreeAccessor;
import step.core.artefacts.reports.ReportTreeVisitor;
import step.core.artefacts.reports.ReportTreeVisitor.ReportNodeEvent;

/**
 * A {@link ReportWriter} that generates JUnit 4 XML reports based on the JUnit schema https://github.com/windyroad/JUnit-Schema/blob/master/JUnit.xsd
 *
 */
public class JUnit4ReportWriter implements ReportWriter {

	private boolean singleTestCase = false;

	public JUnit4ReportWriter() {
	}

	public JUnit4ReportWriter(boolean singleTestCase) {
		this.singleTestCase = singleTestCase;
	}

	@Override
	public void writeReport(ReportTreeAccessor reportTreeAccessor, String executionId, File outputFile) throws IOException {

		try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
			writeReport(reportTreeAccessor, executionId, writer);
		}
	}

	public void writeReport(ReportTreeAccessor reportTreeAccessor, String executionId, Writer writer) throws IOException {
		ReportTreeVisitor visitor = new ReportTreeVisitor(reportTreeAccessor);
		// Using AtomicInteger and StringBuilder because of the "final limitation" in lambdas...
		AtomicInteger numberOfTests = new AtomicInteger(0);
		AtomicInteger numberOfFailures = new AtomicInteger(0);
		AtomicInteger numberOfErrors = new AtomicInteger(0);
		AtomicInteger numberOfSkipped = new AtomicInteger(0);
		AtomicLong duration = new AtomicLong();
		StringBuilder name = new StringBuilder();

		// First visit the report tree to get the root node informations and the different counts
		visitor.visit(executionId, e->{
			if(e.getStack().isEmpty()) {
				name.append(e.getNode().getName());
				duration.set(e.getNode().getDuration());

				// for not test set we only take the root element into accout
				if (singleTestCase && !e.getNode().getResolvedArtefact().isTestSet()) {
					calculateNode(e, numberOfTests, numberOfFailures, numberOfErrors, numberOfSkipped);
				}

			}
			if (e.getStack().size() == 1) {
				if (!singleTestCase || e.getNode().getResolvedArtefact().isTestSet()) {
					calculateNode(e, numberOfTests, numberOfFailures, numberOfErrors, numberOfSkipped);
				}
			}
		});


		writer.write("<testsuite name=\""+name.toString()+"\" time=\""+formatTime(duration.get())+"\" tests=\""+numberOfTests.get()+"\" skipped=\""+numberOfSkipped.get()+"\" failures=\""+numberOfFailures.get()+"\" errors=\""+numberOfErrors.get()+"\">");
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
					if(singleTestCase && event.getStack().isEmpty()){
						if(!event.getNode().getResolvedArtefact().isTestSet()){
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
						if(node.getError() != null) {
							writeErrorOrFailure(writer, node, errorWritten);
						}
					}
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
			}

			private void writeTestCaseBegin(ReportNode node) throws IOException {
				writer.write("<testcase classname=\""+ node.getClass().getName()+"\" name=\""+ node.getName()+"\" time=\""+formatTime(node.getDuration())+"\">");
				errorWritten.set(false);
			}

			private void writeTestCaseEnd() throws IOException {
				writer.write("</testcase>");
				writer.write('\n');
			}

			@Override
			public void endReportNode(ReportNodeEvent event) {
				if (singleTestCase && event.getStack().isEmpty()) {
					if (!event.getNode().getResolvedArtefact().isTestSet()) {
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

                return singleTestCase && !nodeEvent.getStack().isEmpty() && !nodeEvent.getStack().firstElement().getResolvedArtefact().isTestSet();
            }
		});

		writer.write("</testsuite>");
		writer.write('\n');

		writer.flush();
	}

	private static void calculateNode(ReportNodeEvent e, AtomicInteger numberOfTests, AtomicInteger numberOfFailures, AtomicInteger numberOfErrors, AtomicInteger numberOfSkipped) {
		numberOfTests.incrementAndGet();
		ReportNode node = e.getNode();
		if(node.getStatus() == ReportNodeStatus.FAILED) {
			numberOfFailures.incrementAndGet();
		} else if(node.getStatus() == ReportNodeStatus.TECHNICAL_ERROR) {
			numberOfErrors.incrementAndGet();
		} else if(node.getStatus() == ReportNodeStatus.SKIPPED) {
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
				writer.write("<failure type=\"\">"+errorMessage+"</failure>");
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

	protected String formatTime(long duration) {
		DecimalFormat df = new DecimalFormat("0.00"); 
		return df.format(duration/1000.0);
	}

}
