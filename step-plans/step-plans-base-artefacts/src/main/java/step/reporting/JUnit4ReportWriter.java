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

import com.google.common.io.Files;
import org.apache.commons.lang3.StringEscapeUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.TestSet;
import step.attachments.SkippedAttachmentMeta;
import step.attachments.StreamingAttachmentMeta;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * A {@link ReportWriter} that generates JUnit 4 XML reports based on the JUnit schema https://github.com/windyroad/JUnit-Schema/blob/master/JUnit.xsd
 *
 */
public class JUnit4ReportWriter implements ReportWriter {

	private static final String FRONTEND_PATH_TO_EXECUTION_DETAILS = "#/executions/%s/steps";

	private static final Logger log = LoggerFactory.getLogger(JUnit4ReportWriter.class);

	private final Junit4ReportConfig junit4ReportConfig;

	public JUnit4ReportWriter(Junit4ReportConfig junit4ReportConfig) {
		this.junit4ReportConfig = junit4ReportConfig;
	}

	public JUnit4ReportWriter() {
		this.junit4ReportConfig = new Junit4ReportConfig.Builder().createConfig();
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

		String reportFileName = prepareReportFileName();
		ReportAttachmentsInfo aggregatedAttachmentInfo = new ReportAttachmentsInfo();
		for (String executionId : executionIds) {
			ReportGenerationResult generationResult = writeSingleTestSuiteXml(reportTreeAccessor, executionId, writer, true, id, reportFileName);
			aggregatedAttachmentInfo.getAttachmentsPerTestCase().putAll(generationResult.getAttachmentsInfo().getAttachmentsPerTestCase());
			id++;
		}
		writer.write("</testsuites>");
		writer.flush();
		return new ReportMetadata(reportFileName, aggregatedAttachmentInfo);
	}

	/**
	 * @return the file name of report
	 */
	public ReportMetadata writeReport(ReportTreeAccessor reportTreeAccessor, String executionId, Writer writer) throws IOException {
		String reportFileName = prepareReportFileName();
		ReportGenerationResult generationResult = writeSingleTestSuiteXml(reportTreeAccessor, executionId, writer, false, null, reportFileName);
		writer.flush();
		return new ReportMetadata(reportFileName, generationResult.getAttachmentsInfo());
	}

	private ReportGenerationResult writeSingleTestSuiteXml(ReportTreeAccessor reportTreeAccessor, String executionId, Writer writer, boolean writePackage, Integer id, String reportFileName) throws IOException {
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

        // Some systems support custom properties for testsuite, but Gitlab doesn't use and display them
		if (junit4ReportConfig.isAddLinksToStepFrontend()) {
			String linkToStep = generateLinkToStep(executionId);
			if (linkToStep != null) {
				writer.write("<properties>");
				writer.write('\n');
				writer.write("<property name=\"url:step\" value=\"" + linkToStep + "\"/>");
				writer.write('\n');
				writer.write("</properties>");
				writer.write('\n');
			}
		}

		// write test cases
		final int maxErrors = 10;

		// references per test case
		AtomicReference<List<ObjectId>> reportNodesWithErrors = new AtomicReference<>(new ArrayList<>()); // max
		JUnitReportEntryCollector failuresAndErrorsCollector = new JUnitReportEntryCollector();
		ReportAttachmentsInfo attachmentsInfo = new ReportAttachmentsInfo();
		AtomicReference<String> testCaseId = new AtomicReference<>();

		// visit the tree again and write the <testcase> blocks
		visitor.visit(executionId, new ReportNodeVisitorEventHandler() {
			@Override
			public void startReportNode(ReportNodeEvent event) {
				ReportNode node = event.getNode();
				log.debug("{}: {}. Parent: {}", node.getStatus(), node.getId(), node.getParentID());
				try {
					// for test sets we take test cases from the first level
					// for other root nodes we take the top level only
					if(event.getStack().isEmpty()){
						if(!isTestSet(event.getNode())){
							writeTestCaseBegin(node, testSuiteName);
						}
					}

					// as a convention report the children of the first level as testcases
					if(event.getStack().size()==1) {
						if(!skipReportNode(event)) {
							writeTestCaseBegin(node, testSuiteName);
						}
					} else if (event.getStack().size() > 1 && reportNodesWithErrors.get().size() < maxErrors) {
						// report all the errors of the sub nodes (level > 1)
						if (node.getContributingError() != null && node.getContributingError()) {
							if (node.getError() != null) {
								writeErrorOrFailureAndCheckAttachment(node, event.getStack());
							}
						}
					}

				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
			}

			private void writeErrorOrFailureAndCheckAttachment(ReportNode node, Stack<ReportNode> stack) throws IOException {
				for (ReportNode someParent : stack) {
					ObjectId someParentId = someParent.getId();
					if (reportNodesWithErrors.get().contains(someParentId)) {
						log.debug("{}: skipped", node.getId());
						// don't duplicate the error if any of parent nodes is already reported
						return;
					}
				}

				boolean addedInReport = writeErrorOrFailure(node, failuresAndErrorsCollector);
				if (addedInReport) {
					reportNodesWithErrors.get().add(node.getId());

					// add attachment info if exists
					if (node.getAttachments() != null) {
						for (AttachmentMeta attachment : node.getAttachments()) {
							attachmentsInfo.add(testCaseId.get(), attachment);
						}
					}
				}
			}

			private void writeTestCaseBegin(ReportNode node, StringBuilder testSuiteName) throws IOException {
				log.debug("NEW TEST CASE BEGIN: {}", node.getId());
				writer.write("<testcase classname=\"" + testSuiteName + "\" " +
						"name=\"" + node.getName() + "\" " +
						"time=\"" + formatDuration(getTestCaseDuration(node)) + "\">");
				writer.write('\n');

				// cleanup local variables for test case
				testCaseId.set(node.getId().toString());
				failuresAndErrorsCollector.clear();
				reportNodesWithErrors.get().clear();
			}

			private void writeTestCaseEnd(AtomicReference<String> testCaseId, ReportAttachmentsInfo attachmentsInfo,
										  JUnitReportEntryCollector entriesCollector) throws IOException {
				for (JUnitReportEntry mergedEntry : entriesCollector.getMergedEntries()) {
					writer.write(mergedEntry.toXml());
					writer.write('\n');
				}

				// prepare system-out section
				boolean writeSystemOut = false;
				List<AttachmentMeta> attachmentsPerTestCase = attachmentsInfo.getAttachmentsPerTestCase().get(testCaseId.get());
				if (junit4ReportConfig.isAddAttachments() && attachmentsPerTestCase != null && !attachmentsPerTestCase.isEmpty()) {
					writeSystemOut = true;
				}
				if (junit4ReportConfig.isAddLinksToStepFrontend() && generateLinkToStep(executionId) != null) {
					writeSystemOut = true;
				}

				if (writeSystemOut) {
					writer.write("<system-out>");
					writer.write('\n');

					if (junit4ReportConfig.isAddLinksToStepFrontend()) {
						String linkToStep = generateLinkToStep(executionId);
						if (linkToStep != null) {
							writer.write("More details: " + linkToStep);
							writer.write('\n');
							writer.write('\n');
						}
					}

					if (junit4ReportConfig.isAddAttachments() && attachmentsPerTestCase != null && !attachmentsPerTestCase.isEmpty()) {
						for (AttachmentMeta attachmentMeta : attachmentsPerTestCase) {
							writeAttachmentTag(testCaseId.get(), attachmentMeta, writer, reportFileName);
						}
					}
					writer.write("</system-out>");
					writer.write('\n');
				}

				writer.write("</testcase>");
				writer.write('\n');
			}

			@Override
			public void endReportNode(ReportNodeEvent event) {
				if (event.getStack().isEmpty()) {
					if (!isTestSet(event.getNode())) {
						try {
							writeTestCaseEnd(testCaseId, attachmentsInfo, failuresAndErrorsCollector);
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
							if (reportNodesWithErrors.get().isEmpty()) {
								writeErrorOrFailureAndCheckAttachment(node, event.getStack());
							}

							// close the <testcase> block
							writeTestCaseEnd(testCaseId, attachmentsInfo, failuresAndErrorsCollector);
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

	private String generateLinkToStep(String executionId) {
		if (junit4ReportConfig.getServerConfiguration() != null) {
			String controllerUrl = junit4ReportConfig.getServerConfiguration().getProperty("controller.url");
			if (controllerUrl == null) {
				return null;
			}
			if (!controllerUrl.endsWith("/")) {
				controllerUrl = controllerUrl + "/";
			}
			return String.format(controllerUrl + FRONTEND_PATH_TO_EXECUTION_DETAILS, executionId);
		} else {
			return null;
		}
	}

	private void writeAttachmentTag(String testCaseId, AttachmentMeta attachment, Writer writer, String reportFileName) {
		if (junit4ReportConfig.isAddAttachments() && junit4ReportConfig.getAttachmentResourceManager() != null) {
			// Writes tags similar to the following example to the report:
			// [[ATTACHMENT|screenshots/dashboard.png]]
			if (attachment instanceof SkippedAttachmentMeta) {
				log.warn("Unable to add attachment to report {} as the attachment was skipped during execution: id={} name={}", reportFileName, attachment.getId(), attachment.getName());
				return;
			}
			String rootFolder = junit4ReportConfig.getAttachmentsRootFolder();
			if (rootFolder == null) {
				rootFolder = "";
			}
			if (!rootFolder.endsWith(File.separator)) {
				rootFolder += File.separator;
			}

			// all reports prepared as zip are unzipped into the subdirectory with the same name as main xml report (without extension)
			// so for report folder `target\step-reports\` the link should
			// target\step-reports\20241118-173029-281662-junit\attachments\673b4f80cf93b812604f084c\exception.log
			String subfolders = Files.getNameWithoutExtension(reportFileName) + File.separator;
			subfolders += junit4ReportConfig.getAttachmentsSubfolder() == null ? "" : junit4ReportConfig.getAttachmentsSubfolder();
			if(!subfolders.isEmpty()){
				subfolders += File.separator;
			}

			try {
				writer.write(String.format("[[ATTACHMENT|%s%s]]", rootFolder + subfolders + testCaseId + File.separator, attachment.getName()));
				writer.write('\n');
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

	/**
	 * @return true if error or failure is detected
	 */
	protected boolean writeErrorOrFailure(ReportNode node, JUnitReportEntryCollector entriesCollector) {
		String errorMessage = "";
		if(node.getError()!=null && node.getError().getMsg()!=null) {
			errorMessage = node.getError().getMsg();
		}
		// escape special characters in error message
		errorMessage = StringEscapeUtils.escapeXml10(errorMessage);

		if (node.getStatus() != ReportNodeStatus.PASSED) {
			if (isFailure(node)) {
				entriesCollector.add(new JUnitReportEntry(JUnitReportEntry.Type.FAILURE, null, errorMessage));
				log.debug("Add failure: {}", node.getId());
				return true;
			} else if (isError(node)) {
				entriesCollector.add(new JUnitReportEntry(JUnitReportEntry.Type.ERROR, null, errorMessage));
				log.debug("Add error: {}", node.getId());
				return true;
			} else if (isSkipped(node)) {
				// there is only one 'skipped' element per test case allowed (if there are no other failures)
				// so we save the message for 'skipped' and delay the decision until we finish the 'testCase'
				entriesCollector.add(new JUnitReportEntry(JUnitReportEntry.Type.SKIPPED, null, errorMessage));
				return false;
			} else {
				errorMessage = "No error message was reported but the status of the report node was " + node.getStatus().toString();
				entriesCollector.add(new JUnitReportEntry(JUnitReportEntry.Type.ERROR, null, errorMessage));
				log.debug("Add error: {}", node.getId());
				return true;
			}
		}
		return false;
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

	protected static class JUnitReportEntryCollector {

		private final List<JUnitReportEntry> entries = new ArrayList<>();

		public void add(JUnitReportEntry entry) {
			this.entries.add(entry);
		}

		public void clear() {
			this.entries.clear();
		}

		public List<JUnitReportEntry> getMergedEntries() {
			List<JUnitReportEntry> result = new ArrayList<>();

			boolean errorExists = false;
			boolean errorOrFailureExists = false;

			if (entries.stream().anyMatch(e -> e.getType() == JUnitReportEntry.Type.ERROR)) {
				errorExists = true;
			}
			if (entries.stream().anyMatch(e -> e.getType() == JUnitReportEntry.Type.ERROR || e.getType() == JUnitReportEntry.Type.FAILURE)) {
				errorOrFailureExists = true;
			}

			if (errorExists) {
				// In JUnit report we only can include 1 error at max. Gitlab also ignores 'failures' elements if any 'error' exists.
				// So to display all messages in report we aggregate them in single 'error' element
				String allMessages = entries.stream()
						.filter(e -> e.getType() == JUnitReportEntry.Type.ERROR || e.getType() == JUnitReportEntry.Type.FAILURE).map(JUnitReportEntry::getMessage)
						.collect(Collectors.joining("; "));
				result.add(new JUnitReportEntry(JUnitReportEntry.Type.ERROR, null, allMessages));
			} else {
				// If there are no errors, we just add all failures to report
				entries.stream().filter(e -> e.getType() == JUnitReportEntry.Type.FAILURE).forEach(result::add);
			}

			// If in test case there are only skipped nodes and no errors or failures are detected at the same time, then we mark the test case as skipped
			if (!errorOrFailureExists && entries.stream().anyMatch(e -> e.getType() == JUnitReportEntry.Type.SKIPPED)) {
				String mergedSkippedMessages = entries.stream()
						.filter(p -> p.getType() == JUnitReportEntry.Type.SKIPPED && p.getMessage() != null && !p.getMessage().isEmpty())
						.map(JUnitReportEntry::getMessage).collect(Collectors.joining("; "));
				result.add(new JUnitReportEntry(JUnitReportEntry.Type.SKIPPED, null, mergedSkippedMessages.isEmpty() ? null : mergedSkippedMessages));
			}

			return result;
		}
	}

	protected static class JUnitReportEntry {

		private final Type type;

		/**
		 * Not used now
		 */
		private final String typeAttribute;

		private final String message;

		public JUnitReportEntry(Type type, String typeAttribute, String message) {
			this.type = type;
			this.typeAttribute = typeAttribute;
			this.message = message;
		}

		public Type getType() {
			return type;
		}

		public String getTypeAttribute() {
			return typeAttribute;
		}

		public String getMessage() {
			return message;
		}

		public String toXml() {
			StringBuilder resultBuilder = new StringBuilder("<");
			switch (getType()) {
				case ERROR:
					resultBuilder.append("error");
					break;
				case FAILURE:
					resultBuilder.append("failure");
					break;
				case SKIPPED:
					resultBuilder.append("skipped");
					break;
				default:
					throw new UnsupportedOperationException("Unknown junit report entry: " + getType());

			}
			if (getTypeAttribute() != null) {
				resultBuilder.append(String.format(" type=\"%s\"", getTypeAttribute()));
			}
			if (getMessage() != null) {
				resultBuilder.append(String.format(" message=\"%s\"", getMessage()));
			}
			resultBuilder.append("/>");
			return resultBuilder.toString();
		}

		protected enum Type {
			FAILURE,
			ERROR,
			SKIPPED
		}
	}

}
