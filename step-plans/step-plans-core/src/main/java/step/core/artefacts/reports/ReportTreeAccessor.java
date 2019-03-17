package step.core.artefacts.reports;

import java.util.Iterator;

public interface ReportTreeAccessor {

	/**
	 * Get a ReportNode by ID
	 * 
	 * @param id the id of the ReportNode
	 * @return the report node or null if no ReportNode exists with this id
	 */
	public ReportNode get(String id);
	
	/**
	 * Returns the list of children of the ReportNode  
	 * 
	 * @param parentID the ID of the parent ReportNode
	 * @return an Iterator of the list of children
	 */
	public Iterator<ReportNode> getChildren(String parentID);
}
