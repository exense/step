package step.core.scheduler;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.execution.ExecutionRunnable;
import step.core.execution.model.ExecutionParameters;

public class ExecutionScheduler {
	
	private final Logger logger = LoggerFactory.getLogger(ExecutionScheduler.class);
	
	private GlobalContext context;
		
	private Executor executor;
	
	public ExecutionScheduler(GlobalContext globalContext) {
		super();
		
		this.context = globalContext;
		this.executor = new Executor(globalContext);
	}

	public void shutdown() {
		executor.shutdown();
	}

	public void start() {
		executor.start();
		loadExecutionTasks();
	}
	
	private void loadExecutionTasks() {
		Iterator<ExecutiontTaskParameters> it = getActiveExecutionTasks();
		while(it.hasNext()) {
			ExecutiontTaskParameters task = it.next();
			logger.info("Loading schedule: " + task.toString());
			try {
				boolean mayFireAgain = executor.schedule(task);
				if(!mayFireAgain) {
					removeExecutionTask(task.getId());
				}
			} catch (Exception e) {
				logger.error("An error occurred while scheduling task. "+ task.toString()+ ". Disabling task.", e);
				disableExecutionTask(task.getId());
			}
		}
		
	}
	
	public Iterator<ExecutiontTaskParameters> getActiveExecutionTasks() {
		return context.getScheduleAccessor().getActiveExecutionTasks();
	}
	
	public Iterator<ExecutiontTaskParameters> getActiveAndInactiveExecutionTasks() {
		return context.getScheduleAccessor().getActiveAndInactiveExecutionTasks();
	}
	
	public void removeExecutionTask(String executionTaskID) {
		ExecutiontTaskParameters task = get(executionTaskID);
		remove(task);
		executor.deleteSchedule(task);
	}
	
	public void enableExecutionTask(String executionTaskID) {
		ExecutiontTaskParameters task = get(executionTaskID);
		task.setActive(true);
		save(task);
		executor.schedule(task);
	}
	
	public void disableExecutionTask(String executionTaskID) {
		ExecutiontTaskParameters task = get(executionTaskID);
		task.setActive(false);
		save(task);
		executor.deleteSchedule(task);
	}

	public boolean addExecutionTask(ExecutiontTaskParameters task) {
		task.setActive(true);
		save(task);
		return executor.schedule(task);
	}
	
	public String execute(ExecutionParameters executionParameters) {
		return executor.execute(executionParameters);
	}

	public ExecutiontTaskParameters get(String id) {
		return context.getScheduleAccessor().get(id);
	}
	
	private void save(ExecutiontTaskParameters schedule) {
		context.getScheduleAccessor().save(schedule);
	}
	
	private void remove(ExecutiontTaskParameters schedule) {
		context.getScheduleAccessor().remove(schedule);
	}
	
	public List<ExecutionRunnable> getCurrentExecutions() {
		return executor.getCurrentExecutions();
	}
}
