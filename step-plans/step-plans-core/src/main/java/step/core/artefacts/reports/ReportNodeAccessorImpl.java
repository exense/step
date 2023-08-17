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
package step.core.artefacts.reports;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.bson.types.ObjectId;

import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.collections.SearchOrder;


public class ReportNodeAccessorImpl extends AbstractAccessor<ReportNode> implements ReportTreeAccessor, ReportNodeAccessor {
		
	public ReportNodeAccessorImpl(Collection<ReportNode> collectionDriver) {
		super(collectionDriver);
	}

	@Override
	public void createIndexesIfNeeded(Long ttl) {
		createOrUpdateIndex("parentID");
		createOrUpdateIndex("executionTime");
		createOrUpdateCompoundIndex("executionID", "status", "executionTime");
		createOrUpdateCompoundIndex("executionID", "executionTime");
		createOrUpdateCompoundIndex("executionID", "_class");
		createOrUpdateCompoundIndex("executionID", "parentID");
	}

	@Override
	public List<ReportNode> getReportNodePath(ObjectId id) {
		LinkedList<ReportNode> result = new LinkedList<>();
		appendParentNodeToPath(result, get(id));
		return result;
	}
	
	private void appendParentNodeToPath(LinkedList<ReportNode> path, ReportNode node) {
		path.addFirst(node);
		ReportNode parentNode;
		if(node.getParentID()!=null) {
			parentNode = get(node.getParentID());
			if (parentNode != null) {
				appendParentNodeToPath(path, parentNode);
			}
		}
	}

    @Override
	public Iterator<ReportNode> getChildren(ObjectId parentID) {    	
    	return collectionDriver.find(Filters.equals("parentID", parentID), new SearchOrder("executionTime", 1), null, null, 0).iterator();
    }
    
    @Override
	public Iterator<ReportNode> getChildren(ObjectId parentID, int skip, int limit) {   
    	return collectionDriver.find(Filters.equals("parentID", parentID), new SearchOrder("executionTime", 1), skip, limit, 0).iterator();
    }
    
	@Override
	public Stream<ReportNode> getReportNodesByExecutionID(String executionID) {
		assert executionID != null;
		return collectionDriver.findCloseableStream(Filters.equals("executionID", executionID), new SearchOrder("executionTime", 1), null, null, 0);
	}
	
	@Override
	public Stream<ReportNode> getReportNodesByExecutionIDAndClass(String executionID, String class_) {
		assert executionID != null;
		return collectionDriver.findCloseableStream(
				Filters.and(List.of(Filters.equals("executionID", executionID),
						Filters.equals("_class", class_))),
				new SearchOrder("executionTime", 1), null, null, 0);
	}
	
	@Override
	public Iterator<ReportNode> getReportNodesByExecutionIDAndCustomAttribute(String executionID, Map<String, String> customAttributes) {
		assert executionID != null;
		
		List<Filter> filters = new ArrayList<>();
		filters.add(Filters.equals("executionID", executionID));
		
		if(customAttributes!=null) {
			customAttributes.forEach((k, v)->filters.add(Filters.equals("customAttributes."+k, v)));
		}
		return collectionDriver.find(Filters.and(filters), new SearchOrder("executionTime", 1), null, null, 0)
				.iterator();
	}
	
	@Override
	public ReportNode getReportNodeByParentIDAndArtefactID(ObjectId parentID, ObjectId artefactID) {
		assert parentID != null; assert artefactID!=null;
		return collectionDriver.find(
				Filters.and(List.of(Filters.equals("parentID", parentID), Filters.equals("artefactID", artefactID))),
				null, null, null, 0).findFirst().orElse(null);
	}
    
	@Override
	public ReportNode getRootReportNode(String executionID) {
		assert executionID!=null;
		return collectionDriver.find(
				Filters.and(List.of(Filters.equals("executionID", executionID), Filters.equals("parentID", (String) null))),
				null, null, null, 0).findFirst().orElse(null);
	}
	
	public static class ReportNodeStatusReportEntry {
		
		public ReportNodeStatusReportEntry() {
			super();
		}

		public ReportNodeStatusReportEntry(ReportNodeStatus _id, int sum) {
			super();
			this._id = _id;
			this.sum = sum;
		}

		ReportNodeStatus _id;
		
		int sum;

		public ReportNodeStatus get_id() {
			return _id;
		}

		public void set_id(ReportNodeStatus _id) {
			this._id = _id;
		}

		public int getSum() {
			return sum;
		}

		public void setSum(int sum) {
			this.sum = sum;
		}
	}

	@Override
	public Iterator<ReportNode> getChildren(String parentID) {
		return getChildren(new ObjectId(parentID));
	}

	@Override
	public void removeNodesByExecutionID(String executionID) {
		assert executionID != null;
		collectionDriver.remove(Filters.equals("executionID", executionID));
	}
}
