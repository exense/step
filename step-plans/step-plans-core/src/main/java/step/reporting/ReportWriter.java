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
