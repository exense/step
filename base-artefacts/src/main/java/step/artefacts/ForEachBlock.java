package step.artefacts;

import step.artefacts.handlers.ForBlockHandler;
import step.core.artefacts.Artefact;
import step.core.artefacts.DynamicAttribute;

@Artefact(name="ForEach", handler = ForBlockHandler.class)
public class ForEachBlock extends AbstractForBlock {
	
	@DynamicAttribute
	String header = "true";
	
	@DynamicAttribute
	String table;
	
	@DynamicAttribute
	String folder;

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public Boolean getHeader() {
		return header!=null&&header.length()>0?Boolean.valueOf(header):null;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}
}
