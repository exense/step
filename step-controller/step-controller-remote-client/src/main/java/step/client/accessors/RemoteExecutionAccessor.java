package step.client.accessors;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;

import javax.ws.rs.client.Invocation.Builder;

import org.bson.types.ObjectId;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.repositories.RepositoryObjectReference;

public class RemoteExecutionAccessor extends AbstractRemoteClient implements ExecutionAccessor {

	public RemoteExecutionAccessor(ControllerCredentials credentials){
		super(credentials);
	}
	
	public RemoteExecutionAccessor(){
		super();
	}

	@Override
	public Execution get(ObjectId id) {
		Builder b = requestBuilder("/rest/controller/execution/"+id.toString());
		return executeRequest(()->b.get(Execution.class));
	}
	
	@Override
	public Execution get(String executionId) {
		return get(new ObjectId(executionId));
	}

	@Override
	public Execution save(Execution entity) {
		throw notImplemented();
	}

	@Override
	public void save(List<? extends Execution> entities) {
		throw notImplemented();
	}

	@Override
	public Execution findByAttributes(Map<String, String> attributes) {
		throw notImplemented();
	}

	@Override
	public void createIndexesIfNeeded(Long ttl) {
		throw notImplemented();
	}

	@Override
	public List<Execution> getActiveTests() {
		throw notImplemented();
	}

	@Override
	public List<Execution> getTestExecutionsByArtefactURL(RepositoryObjectReference objectReference) {
		throw notImplemented();
	}

	@Override
	public Iterable<Execution> findByCritera(Map<String, Object> criteria, int limit) {
		throw notImplemented();
	}

	@Override
	public Iterable<Execution> findLastStarted(int limit) {
		throw notImplemented();
	}

	@Override
	public Iterable<Execution> findLastEnded(int limit) {
		throw notImplemented();
	}

	@Override
	public void remove(ObjectId id) {
		throw notImplemented();
	}

	@Override
	public Iterator<Execution> getAll() {
		throw notImplemented();
	}

	@Override
	public List<Execution> getLastExecutionsBySchedulerTaskID(String schedulerTaskID, int limit) {
		throw notImplemented();
	}

	@Override
	public Spliterator<Execution> findManyByAttributes(Map<String, String> attributes) {
		throw notImplemented();
	}	
}
