package step.core.artefacts.reports;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import step.commons.datatable.DataTable;
import step.core.accessors.CRUDAccessor;

public interface ReportNodeAccessor extends CRUDAccessor<ReportNode> {

	void createIndexesIfNeeded(Long ttl);

	List<ReportNode> getReportNodePath(ObjectId id);

	Iterator<ReportNode> getChildren(ObjectId parentID);

	Iterator<ReportNode> getChildren(ObjectId parentID, int skip, int limit);

	Iterator<ReportNode> getReportNodesByExecutionID(String executionID);

	long countReportNodesByExecutionID(String executionID);

	Iterator<ReportNode> getReportNodesByExecutionIDAndClass(String executionID, String class_);

	Iterator<ReportNode> getLeafReportNodesByExecutionID(String executionID);

	Iterator<ReportNode> getReportNodesByExecutionIDAndCustomAttribute(String executionID,
			List<Map<String, String>> customAttributes);

	ReportNode getReportNodeByParentIDAndArtefactID(ObjectId parentID, ObjectId artefactID);

	Iterator<ReportNode> getReportNodesByExecutionIDAndArtefactID(String executionID, String artefactID);

	Iterator<ReportNode> getFailedLeafReportNodesByExecutionID(String executionID);

	// TODO check if still working
	DataTable getTimeBasedReport(String executionID, int resolution);

	ReportNode getRootReportNode(String executionID);

	Map<ReportNodeStatus, Integer> getLeafReportNodesStatusDistribution(String executionID, String reportNodeClass);

	Iterator<ReportNode> getChildren(String parentID);

}