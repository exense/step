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

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.TestSet;
import step.reports.CustomReportType;
import step.attachments.AttachmentMeta;
import step.core.artefacts.reports.*;
import step.core.artefacts.reports.ReportTreeVisitor.ReportNodeEvent;
import step.resources.ResourceRevisionFileHandle;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link ReportWriter} that generates JUnit 4 XML reports based on the JUnit schema https://github.com/windyroad/JUnit-Schema/blob/master/JUnit.xsd
 *
 */
public class JUnit4ReportWriter implements ReportWriter {

	private static final Logger log = LoggerFactory.getLogger(JUnit4ReportWriter.class);

	private final AttachmentsConfig attachmentsConfig;

	public JUnit4ReportWriter(AttachmentsConfig attachmentsConfig) {
		this.attachmentsConfig = attachmentsConfig;
	}

	public JUnit4ReportWriter() {
		this.attachmentsConfig = null;
	}

	@Override
	public void writeReport(ReportTreeAccessor reportTreeAccessor, String executionId, File outputFile) throws IOException {
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
			writeReport(reportTreeAccessor, executionId, writer);
		}
	}

	public ReportMetadata writeMultiReport(ReportTreeAccessor reportTreeAccessor, List<String> executionIds, Writer writer) throws IOException {
		writer.write("<testsuites>");
		writer.write("\n");
		int id = 0;

		// TODO: individual attachments per test case? in https://github.com/testmoapp/junitxml?tab=readme-ov-file#attachments attachments are listed in <system-out> within the <testcase>, but schema doesn't allow it
		ReportAttachmentsInfo aggregatedAttachmentInfo = new ReportAttachmentsInfo();
		for (String executionId : executionIds) {
			ReportGenerationResult generationResult = writeSingleTestSuiteXml(reportTreeAccessor, executionId, writer, true, id);
			aggregatedAttachmentInfo.getAttachmentsPerTestCase().putAll(generationResult.getAttachmentsInfo().getAttachmentsPerTestCase());
			id++;
		}
		writer.write("</testsuites>");
		writer.flush();
		return new ReportMetadata(prepareReportFileName(), aggregatedAttachmentInfo);
	}

	/**
	 * @return the file name of report
	 */
	public ReportMetadata writeReport(ReportTreeAccessor reportTreeAccessor, String executionId, Writer writer) throws IOException {
		ReportGenerationResult generationResult = writeSingleTestSuiteXml(reportTreeAccessor, executionId, writer, false, null);
		writer.flush();
		return new ReportMetadata(prepareReportFileName(), generationResult.getAttachmentsInfo());
	}

	private ReportGenerationResult writeSingleTestSuiteXml(ReportTreeAccessor reportTreeAccessor, String executionId, Writer writer, boolean writePackage, Integer id) throws IOException {
		ReportTreeVisitor visitor = new ReportTreeVisitor(reportTreeAccessor);
		// Using AtomicInteger and StringBuilder because of the "final limitation" in lambdas...
		AtomicInteger numberOfTests = new AtomicInteger(0);
		AtomicInteger numberOfFailures = new AtomicInteger(0);
		AtomicInteger numberOfErrors = new AtomicInteger(0);
		AtomicInteger numberOfSkipped = new AtomicInteger(0);
		AtomicLong duration = new AtomicLong();
		AtomicLong executionTime = new AtomicLong();
		StringBuilder testSuiteName = new StringBuilder();

		// First visit the report tree to get the root node informations and the different counts
		visitor.visit(executionId, e->{
			if(e.getStack().isEmpty()) {
				testSuiteName.append(e.getNode().getName());
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

		writer.write("<testsuite name=\"" + testSuiteName.toString() + "\" " +
				(id == null ? "" : "id=\"" + id + "\" ") +
				(writePackage ? "package=\"" + testSuiteName + "\" " : "") +
				"time=\"" + formatDuration(getTestSuiteDuration(duration)) + "\" " +
				"timestamp=\"" + formatTimestamp(getExecutionTime(executionTime)) + "\" " +
				"hostname=\"" + getHostName() + "\" " +
				"tests=\"" + numberOfTests.get() + "\" " +
				"skipped=\"" + numberOfSkipped.get() + "\" " +
				"failures=\"" + numberOfFailures.get() + "\" " +
				"errors=\"" + numberOfErrors.get() + "\">");
		writer.write('\n');
		writer.write("<properties></properties>");
		writer.write('\n');

		AtomicBoolean errorWritten = new AtomicBoolean(false);

		ReportAttachmentsInfo attachmentsInfo = new ReportAttachmentsInfo();
		AtomicReference<String> testCaseId = new AtomicReference<>();

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
							writeTestCaseBegin(node, testSuiteName, testCaseId);
						}
					}

					// as a convention report the children of the first level as testcases
					if(event.getStack().size()==1) {
						if(!skipReportNode(event)) {
							writeTestCaseBegin(node, testSuiteName,  testCaseId);
						}
					} else if (event.getStack().size()>1) {
						// report all the errors of the sub nodes (level > 1)
						if(node.getError() != null && !errorWritten.get()) {
							writeErrorOrFailure(writer, node, errorWritten);
						}
					}

					// add attachment info
					if(node.getAttachments() != null && !node.getAttachments().isEmpty()) {
						attachmentsInfo.add(testCaseId.get(), node.getAttachments());
					}

				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
			}

			private void writeTestCaseBegin(ReportNode node, StringBuilder testSuiteName,
											AtomicReference<String> testCaseId) throws IOException {
				writer.write("<testcase classname=\"" + testSuiteName + "\" " +
						"name=\"" + node.getName() + "\" " +
						"time=\"" + formatDuration(getTestCaseDuration(node)) + "\">");
				writer.write('\n');

				testCaseId.set(node.getId().toString());

				errorWritten.set(false);
			}

			private void writeTestCaseEnd(AtomicReference<String> testCaseId, ReportAttachmentsInfo attachmentsInfo) throws IOException {
				if (attachmentsConfig != null) {
					List<AttachmentMeta> attachmentsPerTestCase = attachmentsInfo.getAttachmentsPerTestCase().get(testCaseId.get());
					if (attachmentsPerTestCase != null && !attachmentsPerTestCase.isEmpty()) {
						writer.write("<system-out>");
						for (AttachmentMeta attachmentMeta : attachmentsPerTestCase) {
							writeAttachmentTag(testCaseId.get(), attachmentMeta, writer);
						}
						writer.write("</system-out>");
						writer.write('\n');
					}
				}

				writer.write("</testcase>");
				writer.write('\n');
			}

			@Override
			public void endReportNode(ReportNodeEvent event) {
				if (event.getStack().isEmpty()) {
					if (!isTestSet(event.getNode())) {
						try {
							writeTestCaseEnd(testCaseId, attachmentsInfo);
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
							writeTestCaseEnd(testCaseId, attachmentsInfo);
						} catch (IOException e1) {
							throw new RuntimeException(e1);
						}
					}
				}
			}

			protected boolean skipReportNode(ReportNodeEvent nodeEvent) {
				Stack<ReportNode> stack = nodeEvent.getStack();
				return !stack.isEmpty() && !(isTestSet(stack.firstElement()));
            }
		});

		writer.write("<system-out>");
		writer.write("</system-out>");
		writer.write('\n');
		writer.write("<system-err></system-err>");
		writer.write('\n');
		writer.write("</testsuite>");
		writer.write('\n');

		return new ReportGenerationResult(attachmentsInfo);
	}

	private void writeAttachmentTag(String testCaseId, AttachmentMeta attachment, Writer writer) {
		if (attachmentsConfig != null && attachmentsConfig.getAttachmentResourceManager() != null) {
			// [[ATTACHMENT|screenshots/dashboard.png]]
			ResourceRevisionFileHandle content = attachmentsConfig.getAttachmentResourceManager().getResourceFile(attachment.getId().toString());

			String subfolders = attachmentsConfig.getAttachmentSubfolder() == null ? "" : attachmentsConfig.getAttachmentSubfolder();
			if(!subfolders.isEmpty()){
				subfolders += File.separator;
			}
			subfolders = subfolders + testCaseId + File.separator;

			// TODO: maybe add timestamp to guarantee unique name
			String attachmentFileName = content.getResourceFile().getName();
			try {
				writer.write(String.format("[[ATTACHMENT|%s%s]]", subfolders, attachmentFileName));
			} catch (Exception ex) {
				log.error("Unable to add the attachment to junit report", ex);
			}
		}
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

	private String prepareReportFileName() {
		// use timestamp instead of plan name, because plan name can contain forbidden characters for file name
		String formattedTimestamp = getCurrentTimestamp();
		String fileName = formattedTimestamp + "-" + CustomReportType.JUNITXML.getNameInFile().toLowerCase();
		return fileName + ".xml";
	}

	public static String getCurrentTimestamp() {
		return DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSSSSS").format(LocalDateTime.now());
	}

	private static boolean isTestSet(ReportNode node) {
		return node.getResolvedArtefact() instanceof TestSet;
	}

	private static void calculateNode(ReportNodeEvent e, AtomicInteger numberOfTests, AtomicInteger numberOfFailures, AtomicInteger numberOfErrors, AtomicInteger numberOfSkipped) {
		numberOfTests.incrementAndGet();
		ReportNode node = e.getNode();
		if(isFailure(node)) {
			numberOfFailures.incrementAndGet();
		} else if(isError(node)) {
			numberOfErrors.incrementAndGet();
		} else if(isSkipped(node)) {
			numberOfSkipped.incrementAndGet();
		}
	}

	private static boolean isError(ReportNode node) {
		return node.getStatus() == ReportNodeStatus.TECHNICAL_ERROR;
	}

	private static boolean isFailure(ReportNode node) {
		return node.getStatus() == ReportNodeStatus.FAILED;
	}

	private static boolean isSkipped(ReportNode node) {
		return node.getStatus() == ReportNodeStatus.SKIPPED || node.getStatus() == ReportNodeStatus.NORUN;
	}

	protected void writeErrorOrFailure(Writer writer, ReportNode node, AtomicBoolean errorWritten) throws IOException {
		String errorMessage = "";
		if(node.getError()!=null && node.getError().getMsg()!=null) {
			errorMessage = node.getError().getMsg();
		}
		// escape special characters in error message
		errorMessage = StringEscapeUtils.escapeXml10(errorMessage);

		if(node.getStatus()!=ReportNodeStatus.PASSED) {
			if(isFailure(node)) {
				writer.write("<failure type=\"\" message=\"" + errorMessage + "\"/>");
				writer.write('\n');
				errorWritten.set(true);
			} else if(isError(node)) {
				writer.write("<error type=\"\">"+errorMessage+"</error>");
				writer.write('\n');
				errorWritten.set(true);
			} else if (isSkipped(node)) {
				writer.write("<skipped message=\"" + errorMessage + "\"/>");
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
		return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").format(LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), getZoneId()));
	}

	protected ZoneId getZoneId() {
		return ZoneId.systemDefault();
	}

	private static class ReportGenerationResult {
		private ReportAttachmentsInfo attachmentsInfo;

		public ReportGenerationResult(ReportAttachmentsInfo attachmentsInfo) {
			this.attachmentsInfo = attachmentsInfo;
		}

		public ReportAttachmentsInfo getAttachmentsInfo() {
			return attachmentsInfo;
		}
	}

}
