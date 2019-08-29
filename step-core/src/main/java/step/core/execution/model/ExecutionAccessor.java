package step.core.execution.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

import step.core.accessors.CRUDAccessor;
import step.core.repositories.RepositoryObjectReference;

public interface ExecutionAccessor extends CRUDAccessor<Execution> {

	void createIndexesIfNeeded(Long ttl);

	Execution get(String nodeId);

	List<Execution> getActiveTests();

	List<Execution> getTestExecutionsByArtefactURL(RepositoryObjectReference objectReference);

	Iterable<Execution> findByCritera(Map<String, String> criteria, Date start, Date end);

	Iterable<Execution> findLastStarted(int limit);

	Iterable<Execution> findLastEnded(int limit);

	List<Execution> getLastExecutionsBySchedulerTaskID(String schedulerTaskID, int limit);

}