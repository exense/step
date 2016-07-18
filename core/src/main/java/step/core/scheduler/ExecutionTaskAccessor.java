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
