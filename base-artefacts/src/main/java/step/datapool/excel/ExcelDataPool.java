package step.datapool.excel;

import step.core.artefacts.DynamicAttribute;


public class ExcelDataPool {

	@DynamicAttribute
	String header;
	
	@DynamicAttribute
	String workbookPath;

	public Boolean getHeader() {
		return header!=null&&header.length()>0?Boolean.valueOf(header):null;
	}

	public String getWorkbookPath() {
		return workbookPath;
	}
}
