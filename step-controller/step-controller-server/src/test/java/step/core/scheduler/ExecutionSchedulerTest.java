package step.core.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import step.core.controller.ControllerSettingAccessor;
import step.core.controller.InMemoryControllerSettingAccessor;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ExecutionParameters;
import step.core.plugins.PluginManager.Builder.CircularDependencyException;

public class ExecutionSchedulerTest {

	private ControllerSettingAccessor controllerSettingAccessor;
	private ExecutionTaskAccessor executionTaskAccessor;		
	private MockedExecutor executor;
	
	public MockedExecutor getExecutor() {
		return this.executor;
	}
	
	public static class MockedExecutor extends Executor {
		private boolean isValidateMethodCalled;
		private boolean isScheduleMethodCalled;
		private boolean isDeleteScheduleMethodCalled;
		private boolean isShutDownMethodCalled;
		private boolean isStartMethodCalled;
		private boolean isExecuteWithExecutionParametersMethodCalled;
		private boolean isExecuteWithExecutionTaskParametersMethodCalled;
		private boolean isGetCurrentSessionMethodCalled;

		public MockedExecutor() {
			super();
			this.isValidateMethodCalled = false;
			this.isScheduleMethodCalled = false;
			this.isDeleteScheduleMethodCalled = false;
			this.isShutDownMethodCalled = false;
			this.isStartMethodCalled = false;
			this.isExecuteWithExecutionParametersMethodCalled = false;
			this.isExecuteWithExecutionTaskParametersMethodCalled = false;
			this.isGetCurrentSessionMethodCalled = false;
		}

		@Override
		public void shutdown() {
			this.isShutDownMethodCalled = true;
		}

		@Override
		public void start() {
			this.isStartMethodCalled = true;
		}

		@Override
		public void validate(ExecutiontTaskParameters task) {
			this.isValidateMethodCalled = true;
		}

		@Override
		public String execute(ExecutionParameters executionParameters) {
			this.isExecuteWithExecutionParametersMethodCalled = true;
			return "true";
		}

		@Override
		public String execute(ExecutiontTaskParameters executionTaskParameters) {
			this.isExecuteWithExecutionTaskParametersMethodCalled = true;
			return "true";
		}

		@Override
		public List<ExecutionContext> getCurrentExecutions() {
			this.isGetCurrentSessionMethodCalled = true;
			return new ArrayList<ExecutionContext>();
		}

		@Override
		public List<ExecutiontTaskParameters> getScheduledExecutions() {
			// TODO Auto-generated method stub
			return super.getScheduledExecutions();
		}			
		
		@Override
		public void deleteSchedule(ExecutiontTaskParameters task) {
			this.isDeleteScheduleMethodCalled = true;
		}

		@Override
		public boolean schedule(ExecutiontTaskParameters task) {
			this.isScheduleMethodCalled = true;
			return this.isScheduleMethodCalled;
		}	
	}
	
	@Before
	public void before() throws InstantiationException, IllegalAccessException, CircularDependencyException, ClassNotFoundException {
		controllerSettingAccessor = new InMemoryControllerSettingAccessor();
		executionTaskAccessor = new InMemoryExecutionTaskAccessor();
		executor = new MockedExecutor();		
		controllerSettingAccessor.updateOrCreateSetting(ExecutionScheduler.SETTING_SCHEDULER_ENABLED, "true");
	}
	
	@Test
	public void test() {
		ExecutionScheduler executionScheduler = new ExecutionScheduler(controllerSettingAccessor, executionTaskAccessor, executor);
		
		executionScheduler.start();
		assertTrue(getExecutor().isStartMethodCalled);
		
		ExecutiontTaskParameters executiontTaskParameters = new ExecutiontTaskParameters();
		executiontTaskParameters.setExecutionsParameters(new ExecutionParameters());
		String executionTaskId = executiontTaskParameters.getId().toString();
		
		assertTrue(executionScheduler.addExecutionTask(executiontTaskParameters));
		
		/*boolean containsExecutionTaskParameters = false;
		Iterator<ExecutiontTaskParameters> it = executionScheduler.getActiveExecutionTasks();
		while(it.hasNext()) {
			if(it.next().getId().toString().equals(executionTaskId)) {
				containsExecutionTaskParameters = true;
				break;
			}
		}
		assertTrue(containsExecutionTaskParameters);*/
		
		executionScheduler.getCurrentExecutions();
		assertTrue(getExecutor().isGetCurrentSessionMethodCalled);
		
		executionScheduler.execute(executiontTaskParameters.getExecutionsParameters());
		assertTrue(getExecutor().isExecuteWithExecutionParametersMethodCalled);
		
		executionScheduler.executeExecutionTask(executionTaskId, "admin");
		assertTrue(getExecutor().isExecuteWithExecutionTaskParametersMethodCalled);
		
		executionScheduler.disableExecutionTask(executionTaskId);
		assertTrue(getExecutor().isDeleteScheduleMethodCalled);
		
		executionScheduler.enableExecutionTask(executionTaskId);
		assertTrue(getExecutor().isScheduleMethodCalled);
		
		executionScheduler.disableAllExecutionTasksSchedule();
		// Reset the isScheduleMethodCalled
		getExecutor().isScheduleMethodCalled = false;
		assertFalse(controllerSettingAccessor.getSettingAsBoolean(ExecutionScheduler.SETTING_SCHEDULER_ENABLED));
		
		ExecutiontTaskParameters executiontTaskParameters2 = new ExecutiontTaskParameters();
		executiontTaskParameters2.setExecutionsParameters(new ExecutionParameters());
		String executionTaskId2 = executiontTaskParameters2.getId().toString();
		assertTrue(executionScheduler.addExecutionTask(executiontTaskParameters2));
		
		executionScheduler.enableExecutionTask(executionTaskId2);
		assertFalse(getExecutor().isScheduleMethodCalled);
		
		executionScheduler.enableAllExecutionTasksSchedule();
		assertTrue(controllerSettingAccessor.getSettingAsBoolean(ExecutionScheduler.SETTING_SCHEDULER_ENABLED));
		
		executionScheduler.removeExecutionTask(executionTaskId);
		assertTrue(getExecutor().isDeleteScheduleMethodCalled);
		
		executionScheduler.shutdown();
		assertTrue(getExecutor().isShutDownMethodCalled);
	}

}
