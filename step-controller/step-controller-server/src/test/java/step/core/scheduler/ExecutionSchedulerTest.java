package step.core.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import step.core.controller.ControllerSettingAccessor;
import step.core.controller.InMemoryControllerSettingAccessor;
import step.core.plugins.PluginManager.Builder.CircularDependencyException;

public class ExecutionSchedulerTest {

	private ControllerSettingAccessor controllerSettingAccessor;
	private ExecutionTaskAccessor executionTaskAccessor;		
	private Executor executor;
	
	private class MockedExecutor extends Executor {

		@Override
		public void deleteSchedule(ExecutiontTaskParameters task) {
		}

		@Override
		public boolean schedule(ExecutiontTaskParameters task) {
			return true;
		}

		public MockedExecutor() {
			super();
		}	
	}
	
	@Before
	public void before() throws InstantiationException, IllegalAccessException, CircularDependencyException, ClassNotFoundException {
		controllerSettingAccessor = new InMemoryControllerSettingAccessor();
		executionTaskAccessor = new InMemoryExecutionTaskAccessor();
		executor = new MockedExecutor();			
	}
	
	@Test
	public void test() {
		controllerSettingAccessor.updateOrCreateSetting(ExecutionScheduler.SETTING_SCHEDULER_ENABLED, "true");
		ExecutionScheduler executionScheduler = new ExecutionScheduler(controllerSettingAccessor, executionTaskAccessor, executor);
		
		executionScheduler.disableAllExecutionTasksSchedule();
		assertFalse(controllerSettingAccessor.getSettingAsBoolean(ExecutionScheduler.SETTING_SCHEDULER_ENABLED));
		
		executionScheduler.enableAllExecutionTasksSchedule();
		assertTrue(controllerSettingAccessor.getSettingAsBoolean(ExecutionScheduler.SETTING_SCHEDULER_ENABLED));
	}

}
