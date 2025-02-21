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

import java.util.*;
import java.util.stream.Stream;

import org.bson.types.ObjectId;

import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.collections.SearchOrder;
import step.core.collections.filters.And;
import step.core.collections.filters.Equals;
import step.core.timeseries.TimeSeriesFilterBuilder;


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
		createOrUpdateCompoundIndex("executionID", "artefactHash");
		createOrUpdateCompoundIndex("executionID", "path");
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
	public Iterator<ReportNode> getChildrenByParentSource(ObjectId parentID, ParentSource parentSource) {
		return collectionDriver.find(Filters.and(List.of(Filters.equals("parentID", parentID), Filters.equals("parentSource", parentSource.name()))), new SearchOrder("executionTime", 1), null, null, 0).iterator();
	}

	@Override
	public Iterator<ReportNode> getChildrenByParentSource(ObjectId parentID, ParentSource parentSource, int skip, int limit) {
		return collectionDriver.find(Filters.and(List.of(Filters.equals("parentID", parentID), Filters.equals("parentSource", parentSource.name()))), new SearchOrder("executionTime", 1), skip, limit, 0).iterator();
	}


	@Override
	public Stream<ReportNode> getReportNodesByExecutionID(String executionID) {
		Objects.requireNonNull(executionID);
		return collectionDriver.findLazy(Filters.equals("executionID", executionID), new SearchOrder("executionTime", 1), null, null, 0);
	}

	@Override
	public Stream<ReportNode> getReportNodesByExecutionID(String executionID, Integer limit) {
		Objects.requireNonNull(executionID);
		return collectionDriver.findLazy(Filters.equals("executionID", executionID), new SearchOrder("executionTime", 1), null, limit, 0);
	}

	@Override
	public Stream<ReportNode> getReportNodesByArtefactHash(String executionId, String artefactPathHash, Integer skip, Integer limit) {
		And filter = filterByExecutionIdAndArtefactHash(executionId, artefactPathHash);
		return collectionDriver.findLazy(filter, null, skip, limit, 0);
	}

	@Override
	public Stream<ReportNode> getReportNodesByArtefactHash(String executionId, String artefactPathHash, Long from, Long to, Integer skip, Integer limit) {
		And filter = filterByExecutionIdAndArtefactHash(executionId, artefactPathHash);
		Filter timeFilter = filerByExecutionTime(from, to);
		return collectionDriver.findLazy(Filters.and(List.of(filter, timeFilter)), null, skip, limit, 0);
	}

	private Filter filerByExecutionTime(Long from, Long to) {
		ArrayList<Filter> filters = new ArrayList();
		if (from != null) {
			filters.add(Filters.gte("executionTime", from));
		}

		if (to != null) {
			filters.add(Filters.lt("executionTime", to));
		}

		return (filters.isEmpty() ? Filters.empty() : Filters.and(filters));
	}



	private static And filterByExecutionIdAndArtefactHash(String executionId, String artefactPathHash) {
		return Filters.and(List.of(Filters.equals("executionID", executionId), artefactPathHashFilter(artefactPathHash)));
	}

	private static Equals artefactPathHashFilter(String artefactPathHash) {
		return Filters.equals("artefactHash", artefactPathHash);
	}

	@Override
	public long countReportNodesByArtefactHash(String executionId, String artefactPathHash) {
		And filter = filterByExecutionIdAndArtefactHash(executionId, artefactPathHash);
		return collectionDriver.count(filter, 1000);
	}

	@Override
	public Stream<ReportNode> getReportNodesByExecutionIDAndClass(String executionID, String class_) {
		Objects.requireNonNull(executionID);
		return collectionDriver.findLazy(
				Filters.and(List.of(Filters.equals("executionID", executionID),
						Filters.equals("_class", class_))),
				new SearchOrder("executionTime", 1), null, null, 0);
	}

	@Override
	public Stream<ReportNode> getReportNodesByExecutionIDAndClass(String executionID, String class_, Integer limit) {
		Objects.requireNonNull(executionID);
		return collectionDriver.findLazy(
				Filters.and(List.of(Filters.equals("executionID", executionID),
						Filters.equals("_class", class_))),
				new SearchOrder("executionTime", 1), null, limit, 0);
	}

	@Override
	public Stream<ReportNode> getReportNodesByExecutionIDAndCustomAttribute(String executionID, Map<String, String> customAttributes) {
		Objects.requireNonNull(executionID);
		
		List<Filter> filters = new ArrayList<>();
		filters.add(Filters.equals("executionID", executionID));
		
		if(customAttributes!=null) {
			customAttributes.forEach((k, v)->filters.add(Filters.equals("customAttributes."+k, v)));
		}
		return collectionDriver.findLazy(Filters.and(filters), new SearchOrder("executionTime", 1), null, null, 0);
	}
	
	@Override
	public ReportNode getReportNodeByParentIDAndArtefactID(ObjectId parentID, ObjectId artefactID) {
		Objects.requireNonNull(parentID);
		Objects.requireNonNull(artefactID);
		return collectionDriver.find(
				Filters.and(List.of(Filters.equals("parentID", parentID), Filters.equals("artefactID", artefactID))),
				null, null, null, 0).findFirst().orElse(null);
	}
    
	@Override
	public ReportNode getRootReportNode(String executionID) {
		Objects.requireNonNull(executionID);
		return collectionDriver.find(
				Filters.and(List.of(Filters.equals("executionID", executionID), Filters.equals("parentID", (String) null))),
				null, null, null, 0).findFirst().orElse(null);
	}

	@Override
	public Iterator<ReportNode> getChildren(String parentID) {
		return getChildren(new ObjectId(parentID));
	}

	@Override
	public Iterator<ReportNode> getChildrenByParentSource(String parentID, ParentSource parentSource) {
		return getChildrenByParentSource(new ObjectId(parentID), parentSource);
	}

	@Override
	public Stream<ReportNode> getReportNodesWithContributingErrors(String executionID) {
		return getReportNodesWithContributingErrors(executionID, null, null);
	}

	@Override
	public void removeNodesByExecutionID(String executionID) {
		Objects.requireNonNull(executionID);
		collectionDriver.remove(Filters.equals("executionID", executionID));
	}

	@Override
	public Stream<ReportNode> getReportNodesWithContributingErrors(String executionId, Integer skip, Integer limit) {
		Objects.requireNonNull(executionId);
		return collectionDriver.find(
				Filters.and(List.of(Filters.equals("executionID", executionId), Filters.equals("contributingError", true))),
				null, skip, limit, 0);
	}

}
