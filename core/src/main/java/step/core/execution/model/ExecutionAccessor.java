package step.core.execution.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.jongo.MongoCollection;

import step.core.accessors.MongoDBAccessorHelper;

import com.mongodb.MongoClient;

public class ExecutionAccessor  {
		
	MongoCollection executions;
	
	public ExecutionAccessor(MongoClient client) {
		super();
		executions = MongoDBAccessorHelper.getCollection(client, "executions");
	}
	
	public ExecutionAccessor() {
		super();
	}

	public Execution get(String nodeId) {
		Execution execution = executions.findOne(new ObjectId(nodeId)).as(Execution.class);
		return execution;
	}
		
	public List<Execution> getActiveTests() {
    	List<Execution> result = new ArrayList<>();
    	for(Execution execution:executions.find("{status: { $ne: 'ENDED' }}").as(Execution.class)) {
    		result.add(execution);
    	}
		return result;
	}
	
	public List<Execution> getTestExecutionsByArtefactURL(String artefactURL) {
		List<Execution> result = new ArrayList<>();
    	for(Execution execution:executions.find("{artefactURL: '" + artefactURL +"'}").as(Execution.class)) {
    		result.add(execution);
    	}
		return result;
	}
	
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
		return executions.find(cleanQuery, criteria.values().toArray()).sort("{ \"endTime\" : -1}").limit(limit).as(Execution.class);
	}
	
	public Iterable<Execution> findLastStarted(int limit) {
		String sort = "{ startTime: -1}";
		return executions.find().sort(sort).limit(limit).as(Execution.class);
	}
	
	public Iterable<Execution> findLastEnded(int limit) {
		String sort = "{ endTime: -1}";
		return executions.find().sort(sort).limit(limit).as(Execution.class);
	}

	public void save(Execution execution) {
		executions.save(execution);
	}
}
