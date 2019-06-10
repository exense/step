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
package step.core.execution.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.bson.types.ObjectId;

import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.MongoClientSession;
import step.core.repositories.RepositoryObjectReference;

public class ExecutionAccessorImpl extends AbstractCRUDAccessor<Execution> implements ExecutionAccessor {
		
	com.mongodb.client.MongoCollection<Document> executions_;

	public ExecutionAccessorImpl(MongoClientSession clientSession) {
		super(clientSession, "executions", Execution.class);
		executions_ = clientSession.getMongoDatabase().getCollection("executions");
	}
	
	@Override
	public void createIndexesIfNeeded(Long ttl) {
		createOrUpdateIndex(executions_, "startTime");
		createOrUpdateIndex(executions_, "description");
		createOrUpdateIndex(executions_, "executionParameters.userID");
		createOrUpdateIndex(executions_, "executionTaskID");
	}

	@Override
	public Execution get(String nodeId) {
		return get(new ObjectId(nodeId));
	}
		
	@Override
	public List<Execution> getActiveTests() {
    	List<Execution> result = new ArrayList<>();
    	for(Execution execution:collection.find("{status: { $ne: 'ENDED' }}").as(Execution.class)) {
    		result.add(execution);
    	}
		return result;
	}
	
	@Override
	public List<Execution> getTestExecutionsByArtefactURL(RepositoryObjectReference objectReference) {
		List<Execution> result = new ArrayList<>();
		collection.find("{executionParameters.artefact: #}", objectReference).as(Execution.class).forEach(e->result.add(e));;
		return result;
	}
	
	@Override
	public Iterable<Execution> findByCritera(Map<String,Object> criteria, int limit) {
		
		String critSeparator = ", ";
		String fieldSeparator = " : ";
		StringBuilder query = new StringBuilder();
		
		query.append("{");
		
		for(Entry<String, Object> e : criteria.entrySet())
		{
			query.append(e.getKey());
			query.append(fieldSeparator);
			query.append("#");
			query.append(critSeparator);
		}
		
		// Remove last ","
		String cleanQuery = null;
		if(query.length() > 1)
			cleanQuery = query.substring(0, query.length() - (critSeparator.length()));
		else
			cleanQuery = query.toString();
		cleanQuery += "}";
		//System.out.println(cleanQuery + ";" + criteria.values());
		return collection.find(cleanQuery, criteria.values().toArray()).sort("{ \"endTime\" : -1}").limit(limit).as(Execution.class);
	}
	
	@Override
	public Iterable<Execution> findLastStarted(int limit) {
		String sort = "{ startTime: -1}";
		return collection.find().sort(sort).limit(limit).as(Execution.class);
	}
	
	@Override
	public Iterable<Execution> findLastEnded(int limit) {
		String sort = "{ endTime: -1}";
		return collection.find().sort(sort).limit(limit).as(Execution.class);
	}
	
	@Override
	public List<Execution> getLastExecutionsBySchedulerTaskID(String schedulerTaskID, int limit) {
		List<Execution> result = new ArrayList<>();
		collection.find("{executionTaskID: #, status: { $eq: 'ENDED' }}", schedulerTaskID).sort("{ endTime: -1}").limit(limit).as(Execution.class).forEach(e->result.add(e));;
		return result;
	}
}
