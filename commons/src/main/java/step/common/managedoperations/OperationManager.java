package step.common.managedoperations;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class OperationManager {
	
	ConcurrentHashMap<Long, Operation> operations = new ConcurrentHashMap<Long, Operation>();
	
	static OperationManager INSTANCE = new OperationManager();
	
	public static OperationManager getInstance() {
		return INSTANCE;
	}

	public void enter(String name, Object details) {
		Operation operation = new Operation(name, new Date(), details);
		operations.put(Thread.currentThread().getId(), operation);
		
	}
	
	public void exit() {
		operations.remove(Thread.currentThread().getId());
	}
	
	public Operation getOperation(Long threadId) {
		return operations.get(threadId);
	}

}
