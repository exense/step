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
package step.client.accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;

import org.bson.types.ObjectId;

import step.client.credentials.ControllerCredentials;
import step.commons.datatable.DataTable;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.artefacts.reports.ReportNodeStatus;

public class RemoteReportNodeAccessorImpl extends AbstractRemoteCRUDAccessorImpl<ReportNode> implements ReportNodeAccessor {

	public RemoteReportNodeAccessorImpl() {
		super("/rest/reportnodes/", ReportNode.class);
	}
	
	public RemoteReportNodeAccessorImpl(ControllerCredentials credentials) {
		super(credentials, "/rest/reportnodes/", ReportNode.class);
	}

	@SuppressWarnings("serial")
	private static class ReportNodeArrayList extends ArrayList<ReportNode> {
		
	}
	
	@Override
	public void save(Collection<? extends ReportNode> entities) {
		entities.forEach(r->System.out.println("ReportNode:"+r.getId().toString()));
		Builder b = requestBuilder(path+"many");
		// Workaround: jackson doesn't seem to be able to get the type info of the
		// element of the generic entities collection. Thus using the workaround suggested in
		// https://github.com/FasterXML/jackson-databind/issues/336
		ReportNodeArrayList wrapper = new ReportNodeArrayList();
		wrapper.addAll(entities);
		Entity<?> entity = Entity.entity(wrapper, MediaType.APPLICATION_JSON);
		executeRequest(()->b.post(entity));
	}

	@Override
	public void createIndexesIfNeeded(Long ttl) {
		throw notImplemented();
	}

	@Override
	public List<ReportNode> getReportNodePath(ObjectId id) {
		throw notImplemented();
	}

	@Override
	public Iterator<ReportNode> getChildren(ObjectId parentID) {
		throw notImplemented();
	}

	@Override
	public Iterator<ReportNode> getChildren(ObjectId parentID, int skip, int limit) {
		throw notImplemented();
	}

	@Override
	public Iterator<ReportNode> getReportNodesByExecutionID(String executionID) {
		throw notImplemented();
	}

	@Override
	public long countReportNodesByExecutionID(String executionID) {
		throw notImplemented();
	}

	@Override
	public Iterator<ReportNode> getReportNodesByExecutionIDAndClass(String executionID, String class_) {
		throw notImplemented();
	}

	@Override
	public Iterator<ReportNode> getLeafReportNodesByExecutionID(String executionID) {
		throw notImplemented();
	}

	@Override
	public Iterator<ReportNode> getReportNodesByExecutionIDAndCustomAttribute(String executionID,
			List<Map<String, String>> customAttributes) {
		throw notImplemented();
	}

	@Override
	public ReportNode getReportNodeByParentIDAndArtefactID(ObjectId parentID, ObjectId artefactID) {
		throw notImplemented();
	}

	@Override
	public Iterator<ReportNode> getReportNodesByExecutionIDAndArtefactID(String executionID, String artefactID) {
		throw notImplemented();
	}

	@Override
	public Iterator<ReportNode> getFailedLeafReportNodesByExecutionID(String executionID) {
		throw notImplemented();
	}

	@Override
	public DataTable getTimeBasedReport(String executionID, int resolution) {
		throw notImplemented();
	}

	@Override
	public ReportNode getRootReportNode(String executionID) {
		throw notImplemented();
	}

	@Override
	public Map<ReportNodeStatus, Integer> getLeafReportNodesStatusDistribution(String executionID, String reportNodeClass) {
		throw notImplemented();
	}

	@Override
	public Iterator<ReportNode> getChildren(String parentID) {
		throw notImplemented();
	}

	@Override
	public void removeNodesByExecutionID(String executionID) {
		throw notImplemented();
	}
}
