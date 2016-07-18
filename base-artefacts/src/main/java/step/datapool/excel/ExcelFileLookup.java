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
