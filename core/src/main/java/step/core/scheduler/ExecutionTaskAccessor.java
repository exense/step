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
package step.core.scheduler;

import java.util.Iterator;

import org.bson.types.ObjectId;
import org.jongo.MongoCollection;

import step.core.accessors.MongoDBAccessorHelper;

import com.mongodb.MongoClient;

public class ExecutionTaskAccessor  {
		
	MongoCollection collection;
	
	public ExecutionTaskAccessor(MongoClient client) {
		super();
		collection = MongoDBAccessorHelper.getCollection(client, "tasks");
	}
	
	public ExecutiontTaskParameters get(String nodeId) {
		return collection.findOne(new ObjectId(nodeId)).as(ExecutiontTaskParameters.class);
	}
		
	public Iterator<ExecutiontTaskParameters> getActiveAndInactiveExecutionTasks() {
    	return collection.find().as(ExecutiontTaskParameters.class).iterator();
	}
	
	public Iterator<ExecutiontTaskParameters> getActiveExecutionTasks() {
    	return collection.find("{active:true}").as(ExecutiontTaskParameters.class).iterator();
	}
	
	public void save(ExecutiontTaskParameters schedule) {
		collection.save(schedule);
	}
	
	public void remove(ExecutiontTaskParameters schedule) {
		collection.remove(new ObjectId(schedule.getId()));
	}
}
