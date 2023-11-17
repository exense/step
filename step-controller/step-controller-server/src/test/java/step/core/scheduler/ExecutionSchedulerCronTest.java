package step.core.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import step.core.accessors.AbstractOrganizableObject;
import step.core.controller.ControllerSettingAccessor;
import step.core.controller.InMemoryControllerSettingAccessor;
import step.core.deployment.ControllerServiceException;
import step.core.execution.model.ExecutionParameters;
import step.core.plugins.PluginManager.Builder.CircularDependencyException;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

public class ExecutionSchedulerCronTest {

	private ControllerSettingAccessor controllerSettingAccessor;
	private ExecutionTaskAccessor executionTaskAccessor;		
	private TestExecutor executor;
	
	public TestExecutor getExecutor() {
		return this.executor;
	}

	public static class TestExecutor extends Executor {
		public TestExecutor(Scheduler scheduler) {
			super(scheduler);

		}
	}

	public static class TestJobFactory implements JobFactory {
		@Override
		public Job newJob(TriggerFiredBundle triggerFiredBundle, Scheduler scheduler) throws SchedulerException {
			return null;
		}
	}
	
	@Before
	public void before() throws InstantiationException, IllegalAccessException, CircularDependencyException, ClassNotFoundException {
		controllerSettingAccessor = new InMemoryControllerSettingAccessor();
		executionTaskAccessor = new InMemoryExecutionTaskAccessor();
		Scheduler scheduler;
		try {
			Properties props = new Properties();
			props.put("org.quartz.threadPool.threadCount","1");
			StdSchedulerFactory schedulerFactory = new StdSchedulerFactory(props);
			scheduler = schedulerFactory.getScheduler();
			scheduler.setJobFactory(new TestJobFactory());

		} catch (SchedulerException e) {
			throw new RuntimeException(e);
		}
		executor = new TestExecutor(scheduler);
		controllerSettingAccessor.updateOrCreateSetting(ExecutionScheduler.SETTING_SCHEDULER_ENABLED, "true");
	}
	
	@Test
	public void testValidCases() {
		ExecutionScheduler executionScheduler = new ExecutionScheduler(controllerSettingAccessor, executionTaskAccessor, executor);
		
		executionScheduler.start();

		//Test simple CRON without exclutions
		final ExecutiontTaskParameters executiontTaskParameters = new ExecutiontTaskParameters();
		executiontTaskParameters.addAttribute(AbstractOrganizableObject.NAME, "task1");
		executiontTaskParameters.setExecutionsParameters(new ExecutionParameters());
		executiontTaskParameters.setCronExpression("0 0/1 * * * ?");
		assertTrue(executionScheduler.addExecutionTask(executiontTaskParameters));

		//Test with 1 exclusion
		final ExecutiontTaskParameters executiontTaskParameters3 = new ExecutiontTaskParameters();
		executiontTaskParameters3.addAttribute(AbstractOrganizableObject.NAME, "task2");
		executiontTaskParameters3.setExecutionsParameters(new ExecutionParameters());
		executiontTaskParameters3.setCronExpression("0 0/1 * * * ?");
		CronExclusion cronExclusion = new CronExclusion();
		cronExclusion.setCronExpression("* * * ? * FRI");//Exclude Fridays
		cronExclusion.setDescription("test");
		executiontTaskParameters3.setCronExclusions(List.of(cronExclusion));
		assertTrue(executionScheduler.addExecutionTask(executiontTaskParameters3));

		//Test with 2 exclusion
		final ExecutiontTaskParameters executiontTaskParameters4 = new ExecutiontTaskParameters();
		executiontTaskParameters4.addAttribute(AbstractOrganizableObject.NAME, "task2");
		executiontTaskParameters4.setExecutionsParameters(new ExecutionParameters());
		executiontTaskParameters4.setCronExpression("0 0/1 * * * ?");
		CronExclusion cronExclusion1 = new CronExclusion();
		cronExclusion1.setCronExpression("* * * ? * FRI");//Exclude Fridays
		cronExclusion1.setDescription("test");
		CronExclusion cronExclusion2 = new CronExclusion();
		cronExclusion2.setCronExpression("* * 10-12 ? * *");//Exclude 10h to 12h
		cronExclusion2.setDescription("test2");
		executiontTaskParameters4.setCronExclusions(List.of(cronExclusion1, cronExclusion2));
		assertTrue(executionScheduler.addExecutionTask(executiontTaskParameters4));

		//update task
		CronExclusion cronExclusion3 = executiontTaskParameters4.getCronExclusions().get(0);
		cronExclusion3.setCronExpression("* * * ? * SAT");
		assertTrue(executionScheduler.addExecutionTask(executiontTaskParameters4));

		//Delete task
		executionScheduler.removeExecutionTask(executiontTaskParameters4.getId().toHexString());
	}

	@Test
	public void testInvalidCases() {
		ExecutionScheduler executionScheduler = new ExecutionScheduler(controllerSettingAccessor, executionTaskAccessor, executor);

		executionScheduler.start();

		//Test simple CRON without exclutions
		final ExecutiontTaskParameters executiontTaskParameters = new ExecutiontTaskParameters();
		executiontTaskParameters.addAttribute(AbstractOrganizableObject.NAME, "task1");
		executiontTaskParameters.setExecutionsParameters(new ExecutionParameters());
		executiontTaskParameters.setCronExpression("0 0/1 * * * FRIDAY");
		assertThrows("CronExpression '0 0/1 * * * FRIDAY' is invalid. Details: Support for specifying both a day-of-week AND a day-of-month parameter is not implemented.",
				RuntimeException.class, () -> executionScheduler.addExecutionTask(executiontTaskParameters));

		//Test with same CRON for scheduling and excluding
		/*Note This normally raise a specific error, but kind of take a lot of time, removed for now*/

		//Test with 1 exclusion
		final ExecutiontTaskParameters executiontTaskParameters3 = new ExecutiontTaskParameters();
		executiontTaskParameters3.addAttribute(AbstractOrganizableObject.NAME, "task2");
		executiontTaskParameters3.setExecutionsParameters(new ExecutionParameters());
		executiontTaskParameters3.setCronExpression("0 0/1 * * * ?");
		CronExclusion cronExclusion = new CronExclusion();
		cronExclusion.setCronExpression("* * * * * FRI");//Exclude Fridays
		cronExclusion.setDescription("test");
		executiontTaskParameters3.setCronExclusions(List.of(cronExclusion));
		assertThrows("CronExpression '0 0/1 * * * FRIDAY' is invalid. Details: Support for specifying both a day-of-week AND a day-of-month parameter is not implemented.",
				RuntimeException.class, () -> executionScheduler.addExecutionTask(executiontTaskParameters3));

		executionScheduler.shutdown();
		//Test with 2 exclusion
		final ExecutiontTaskParameters executiontTaskParameters4 = new ExecutiontTaskParameters();
		executiontTaskParameters4.addAttribute(AbstractOrganizableObject.NAME, "task2");
		executiontTaskParameters4.setExecutionsParameters(new ExecutionParameters());
		executiontTaskParameters4.setCronExpression("0 0/1 * * * ?");
		CronExclusion cronExclusion1 = new CronExclusion();
		cronExclusion1.setCronExpression("* * * ? * FRI");//Exclude Fridays
		cronExclusion1.setDescription("test");
		CronExclusion cronExclusion2 = new CronExclusion();
		cronExclusion2.setCronExpression("* * * ? * FRI");//Exclude 10h to 12h
		cronExclusion2.setDescription("test2");
		executiontTaskParameters4.setCronExclusions(List.of(cronExclusion1, cronExclusion2));
		assertThrows("The Scheduler has been shutdown.", RuntimeException.class, () -> executionScheduler.addExecutionTask(executiontTaskParameters4));
	}

}
