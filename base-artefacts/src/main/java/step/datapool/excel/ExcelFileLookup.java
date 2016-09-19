/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.datapool.excel;

import java.io.File;
import java.util.regex.Pattern;

import step.core.artefacts.handlers.ArtefactHandler;
import step.core.execution.ExecutionContext;

public class ExcelFileLookup {

	public static File lookup(String workbookPath) {
		File workBookFile;
		if(workbookPath.contains("/")||workbookPath.contains("\\")) {
			workBookFile = new File(workbookPath);
		} else {			
			Object o = null;
			if(workbookPath.isEmpty()) {
				Pattern excelFilenamePattern = Pattern.compile("^.*\\.xls(x)?$");
				o = ExecutionContext.getCurrentContext().getVariablesManager().getFirstVariableMatching(excelFilenamePattern);
			} else {
				o = ExecutionContext.getCurrentContext().getVariablesManager().getVariable(ArtefactHandler.FILE_VARIABLE_PREFIX + workbookPath);
			}
			
			if(o!=null && o instanceof File) {
				workBookFile = (File) o;
			} else {
				throw new RuntimeException("The workbook '" + workbookPath + "' couldn't be found.");
			}
		}
		return workBookFile;
	}
	
}
