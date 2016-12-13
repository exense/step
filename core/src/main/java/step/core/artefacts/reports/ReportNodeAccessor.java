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
package step.core.artefacts.reports;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.jongo.MongoCollection;
import org.jongo.ResultHandler;

import com.mongodb.DBObject;
import com.mongodb.MongoClient;

import step.commons.datatable.DataTable;
import step.commons.datatable.TableRow;
import step.core.accessors.AbstractAccessor;
import step.core.accessors.MongoDBAccessorHelper;


public class ReportNodeAccessor extends AbstractAccessor {
		
	MongoCollection reports;
	
	com.mongodb.client.MongoCollection<Document> reports_;
	
	public ReportNodeAccessor() {
		super();
	}

	public ReportNodeAccessor(MongoClient client) {
		super();
		reports = MongoDBAccessorHelper.getCollection(client, "reports");
		reports_ = MongoDBAccessorHelper.getMongoCollection_(client, "reports");
	}
	
	public void createIndexesIfNeeded(Long ttl) {
		createOrUpdateIndex(reports_, "parentID");
		createOrUpdateCompoundIndex(reports_, "executionID", "status", "executionTime");
		createOrUpdateCompoundIndex(reports_, "executionID", "executionTime");
		createOrUpdateCompoundIndex(reports_, "executionID", "_class");
	}

	public void save(ReportNode node) {
		reports.save(node);
	}
	
	public ReportNode get(ObjectId nodeId) {
		return reports.findOne(nodeId).as(ReportNode.class);
	}
	
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

    public Iterator<ReportNode> getChildren(ObjectId parentID) {    	
    	return reports.find("{parentID: #}",parentID).sort("{executionTime: 1}").as(ReportNode.class).iterator();
    }
    
	public Iterator<ReportNode> getReportNodesByExecutionID(String executionID) {
		assert executionID != null;
		return reports.find("{executionID: #}", executionID).sort("{executionTime: 1}").as(ReportNode.class).iterator();
	}
	
	public long countReportNodesByExecutionID(String executionID) {
		assert executionID != null;
		return reports.count("{executionID: #}");
	}

	public Iterator<ReportNode> getReportNodesByExecutionIDAndClass(String executionID, String class_) {
		assert executionID != null;
		return reports.find(
				"{executionID: #, _class: #}", executionID, class_).as(ReportNode.class).iterator();
	}
    
	public Iterator<ReportNode> getLeafReportNodesByExecutionID(String executionID) {
		assert executionID != null;
		return reports.find(
				"{executionID: #, $or: [ { _class: 'step.commons.model.report.CallFunctionReportNode' }, { status: 'TECHNICAL_ERROR'} ]}", executionID)
				.sort("{executionTime: 1}").as(ReportNode.class).iterator();
	}
	
//	public Iterator<ReportNode> getLeafReportNodesByExecutionIDAndArtefactID(String executionID, String artefactID) {
//		assert executionID != null; assert artefactID!=null;
//		Iterator<ReportNode> it = getReportNodesByExecutionIDAndArtefactID(executionID, artefactID);
//		Iterator<ReportNode> treeIt = new TreeIterator<ReportNode>(it, new TreeIteratorFactory<ReportNode>() {
//			@Override
//			public Iterator<ReportNode> getChildrenIterator(ReportNode parent) {
//				return getChildren(parent._id);
//			}});
//		Iterator<ReportNode> filterIt = new FilterIterator<ReportNode>(treeIt, new ObjectFilter<ReportNode>() {
//			@Override
//			public boolean matches(ReportNode o) {
//				return ((o instanceof CallFunctionReportNode) || o.getStatus() == ReportNodeStatus.TECHNICAL_ERROR); 
//			}
//		});
//		return filterIt;
//	}
	
	public Iterator<ReportNode> getLeafReportNodesByExecutionIDAndCustomAttribute(String executionID, List<Map<String, String>> customAttributes) {
		assert executionID != null;
		
		StringBuilder query = new StringBuilder();
		query.append("{$and:[{executionID: #},");
		
		if(customAttributes!=null&&customAttributes.size()>0) {
			query.append("{$or: [");
			Iterator<Map<String, String>> it = customAttributes.iterator();
			while(it.hasNext()) {
				Map<String, String> customAttributeMap = it.next();
				Iterator<String> keyIt = customAttributeMap.keySet().iterator();
				while(keyIt.hasNext()) {
					String key = keyIt.next();
					query.append("{'customAttributes.").append(key).append("': '").append(customAttributeMap.get(key)).append("'}");
					if(keyIt.hasNext()) {
						query.append(",");
					}
				}
				if(it.hasNext()) {
					query.append(",");
				}
			}
			query.append("]},");
		}
		query.append("{$or: [ { _class: 'step.artefacts.reports.CallFunctionReportNode' }, { status: 'TECHNICAL_ERROR'} ]}]}");
		
		return reports.find(query.toString(), executionID).sort("{executionTime: 1}").as(ReportNode.class).iterator();
	}
	
	public ReportNode getReportNodeByParentIDAndArtefactID(ObjectId parentID, ObjectId artefactID) {
		assert parentID != null; assert artefactID!=null;
		return reports.findOne("{parentID: #, artefactID: #}", parentID, artefactID).as(ReportNode.class);
	}
	
	public Iterator<ReportNode> getReportNodesByExecutionIDAndArtefactID(String executionID, String artefactID) {
		assert executionID != null; assert artefactID!=null;
		return reports.find("{executionID: #, artefactID: #}", executionID, new ObjectId(artefactID)).as(ReportNode.class).iterator();
	}
	
	public Iterator<ReportNode> getFailedLeafReportNodesByExecutionID(String executionID) {
		assert executionID != null;
		return reports.find("{executionID: #, $or: [ { _class: 'step.commons.model.report.CallFunctionReportNode', status: {$ne:'PASSED'}}, { status: 'TECHNICAL_ERROR'} ]}", executionID)
				.sort("{executionTime: 1}").as(ReportNode.class).iterator();
	}
	
	// TODO check if still working
	public DataTable getTimeBasedReport(String executionID, int resolution) {
		String reportNodeClass = "step.artefacts.reports.CallFunctionReportNode";
		DataTable t = new DataTable();
		final double normalizationFactor = (1.0*resolution)/1000;
				
		reports.aggregate("{$match:{executionID:'"+executionID+"',_class:'"+reportNodeClass+"'}}").
				and("{$group:{_id:{time:{$subtract:[\"$executionTime\",{$mod:[\"$executionTime\","+resolution+"]}]}},value:{$sum:1}}}").
				and("{$sort:{\"_id\":1}}").map(new ResultHandler<TableRow>() {
			@Override
			public TableRow map(DBObject result) {
				Date date = new Date((long) ((DBObject)result.get("_id")).get("time"));
				double value = new Double((Integer) result.get("value"))/normalizationFactor;
				TableRow r = new TableRow(date, value);
				return r;
			}
		}).forEach(row->t.addRow(row));		
		return t;
	}
    
	public ReportNode getRootReportNode(String executionID) {
		assert executionID!=null;
		return reports.findOne("{executionID: '" + executionID + "', parentID: null}").as(ReportNode.class);
	}
	
	public Map<ReportNodeStatus, Integer> getLeafReportNodesStatusDistribution(String executionID, String reportNodeClass) {
		HashMap<ReportNodeStatus, Integer> result = new HashMap<ReportNodeStatus, Integer>();
		for(ReportNodeStatusReportEntry entry:reports.aggregate("{$match:{executionID:'"+executionID+"',_class:'"+reportNodeClass+"'}}").
				and("{$group:{_id:'$status',sum:{$sum:1}}}").as(ReportNodeStatusReportEntry.class)) {
			result.put(entry._id, entry.sum);
		}
		return result;
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
}
