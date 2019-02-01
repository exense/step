package step.reporting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
	
	@Override
	public void writeReport(ReportTreeAccessor reportTreeAccessor, String executionId, File outputFile) throws IOException {
		ReportTreeVisitor visitor = new ReportTreeVisitor(reportTreeAccessor);
		
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
			// Using AtomicInteger and StringBuilder because of the "final limitation" in lambdas...
			AtomicInteger numberOfTests = new AtomicInteger(0);
			AtomicInteger numberOfFailures = new AtomicInteger(0);
			AtomicInteger numberOfErrors = new AtomicInteger(0);
			AtomicInteger numberOfSkipped = new AtomicInteger(0);
			AtomicLong duration = new AtomicLong();
			StringBuilder name = new StringBuilder();
			
			// First visit the report tree to get the root node informations and the different counts
			visitor.visit(executionId, e->{
				if(e.getStack().size()==0) {
					name.append(e.getNode().getName());
					duration.set(e.getNode().getDuration());
				}
				if(e.getStack().size()==1) {
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
			});
			
			
			writer.write("<testsuite name=\""+name.toString()+"\" time=\""+formatTime(duration.get())+"\" tests=\""+numberOfTests.get()+"\" skipped=\""+numberOfSkipped.get()+"\" failures=\""+numberOfFailures.get()+"\" errors=\""+numberOfErrors.get()+"\">");
			writer.newLine();
			
			AtomicBoolean errorWritten = new AtomicBoolean(false);
			// visit the tree again and write the <testcase> blocks
			visitor.visit(executionId, new ReportNodeVisitorEventHandler() {
				@Override
				public void startReportNode(ReportNodeEvent event) {
					ReportNode node = event.getNode();
					try {
						// as a convention report the children of the first level as testcases
						if(event.getStack().size()==1) {
							if(!skipReportNode(node)) {
								writer.write("<testcase classname=\""+node.getClass().getName()+"\" name=\""+node.getName()+"\" time=\""+formatTime(node.getDuration())+"\">");
								errorWritten.set(false);
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
				
				@Override
				public void endReportNode(ReportNodeEvent event) {
					if(event.getStack().size()==1) {
						ReportNode node = event.getNode();
						if(!skipReportNode(node)) {
							try {
								// if no error has been found in the sub-nodes, report the error for this node
								if(node.getStatus()!=ReportNodeStatus.PASSED && !errorWritten.get()) {
									writeErrorOrFailure(writer, node, errorWritten);
								}
								// close the <testcase> block
								writer.write("</testcase>");
								writer.newLine();
							} catch (IOException e1) {
								throw new RuntimeException(e1);
							}
						}
					}
				}

				protected boolean skipReportNode(ReportNode node) {
					return node.getStatus()==ReportNodeStatus.SKIPPED || node.getStatus()==ReportNodeStatus.NORUN;
				}
			});
			
			writer.write("</testsuite>");
			writer.newLine();
			
			writer.flush();
		}
	}

	protected void writeErrorOrFailure(BufferedWriter writer, ReportNode node, AtomicBoolean errorWritten) throws IOException {
		String errorMessage = "";
		if(node.getError()!=null && node.getError().getMsg()!=null) {
			errorMessage = node.getError().getMsg();
		}

		if(node.getStatus()!=ReportNodeStatus.PASSED) {
			if(node.getStatus() == ReportNodeStatus.FAILED) {
				writer.write("<failure type=\"\">"+errorMessage+"</failure>");
				writer.newLine();
				errorWritten.set(true);
			} else if(node.getStatus() == ReportNodeStatus.TECHNICAL_ERROR) {
				writer.write("<error type=\"\">"+errorMessage+"</error>");
				writer.newLine();
				errorWritten.set(true);
			} else {
				writer.write("<error type=\"\">No error message was reported but the status of the report node was "+node.getStatus().toString()+"</error>");
				writer.newLine();
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
