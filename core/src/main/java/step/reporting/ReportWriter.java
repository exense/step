package step.reporting;

import java.io.File;
import java.io.IOException;

import step.core.artefacts.reports.ReportTreeAccessor;

/**
 * A {@link ReportWriter} is responsible to the generation and writing of reports for 
 * plan executions based on their report node trees
 *
 */
public interface ReportWriter {

	/**
	 * Writes the report of the provided execution 
	 * 
	 * @param reportTreeAccessor the {@link ReportTreeAccessor} to be used to access the report node tree
	 * @param executionId the ID of the execution to be reported
	 * @param outputFile the output file the report has to be written to
	 * @throws IOException if an error occurs while writing the report
	 */
	public void writeReport(ReportTreeAccessor reportTreeAccessor, String executionId, File outputFile) throws IOException;
}
