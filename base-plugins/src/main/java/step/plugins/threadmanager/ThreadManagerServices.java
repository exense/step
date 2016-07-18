package step.plugins.threadmanager;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import step.common.managedoperations.Operation;
import step.core.deployment.AbstractServices;
import step.core.execution.ExecutionRunnable;

@Path("/threadmanager")
public class ThreadManagerServices extends AbstractServices {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/operations")
	public List<Operation> getCurrentOperations(@QueryParam("eid") String executionID) {
		ThreadManager threadManager = (ThreadManager) getContext().get(ThreadManager.THREAD_MANAGER_INSTANCE_KEY);

		ExecutionRunnable task = getExecutionRunnable(executionID);
		if(task!=null) {
			return threadManager.getCurrentOperations(task.getContext());
		} else {
			return new ArrayList<Operation>();
		}
	}
}
