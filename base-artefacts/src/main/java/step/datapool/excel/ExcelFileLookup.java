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

import step.attachments.FileResolver;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.execution.ExecutionContext;

public class ExcelFileLookup {

	ExecutionContext context;
	FileResolver fileResolver;
	
	public ExcelFileLookup(ExecutionContext context) {
		super();
		this.context = context;
		this.fileResolver = context.get(FileResolver.class);
	}

	public File lookup(String workbookPath) {
		File workBookFile = fileResolver.resolve(workbookPath);
		if(workBookFile == null || !workBookFile.exists()) {
			if(context!=null) {
				Object o = null;
				if(workbookPath.isEmpty()) {
					Pattern excelFilenamePattern = Pattern.compile("^.*\\.xls(x)?$");
					o = context.getVariablesManager().getFirstVariableMatching(excelFilenamePattern);
				} else {
					o = context.getVariablesManager().getVariable(ArtefactHandler.FILE_VARIABLE_PREFIX + workbookPath);
				}
				
				if(o!=null && o instanceof File) {
					workBookFile = (File) o;
				} else {
					throw new RuntimeException("The workbook '" + workbookPath + "' couldn't be found.");
				}
				
			} else {
				throw new RuntimeException("Unable to lookup workbook '" + workbookPath + "' because the context is null. This should never happen");
			}
		}
		return workBookFile;
	}
	
}
