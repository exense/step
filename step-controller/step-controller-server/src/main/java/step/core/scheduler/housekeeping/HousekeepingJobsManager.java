package step.core.scheduler.housekeeping;

import ch.exense.commons.app.Configuration;
import org.bson.types.ObjectId;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.controller.ControllerSetting;
import step.core.controller.ControllerSettingAccessor;
import step.core.settings.SettingHook;
import step.core.settings.SettingHookRollbackException;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static org.quartz.TriggerBuilder.newTrigger;

public class HousekeepingJobsManager {

	private static final Logger log = LoggerFactory.getLogger(HousekeepingJobsManager.class);

	private final Scheduler scheduler;
	private final ControllerSettingAccessor controllerSettingAccessor;
	private final HousekeepingJobFactory housekeepingJobFactory;

	private final Map<String, SettingHook> hooks = new ConcurrentHashMap<>();

	public HousekeepingJobsManager(Configuration configuration, ControllerSettingAccessor controllerSettingAccessor) throws SchedulerException {

		this.controllerSettingAccessor = controllerSettingAccessor;

		Properties props = new Properties();
		props.put("org.quartz.threadPool.threadCount", configuration.getProperty("housekeeping.scheduler.threads", "2"));
		props.put("org.quartz.scheduler.instanceName", "HousekeepingScheduler");
		StdSchedulerFactory stdSchedulerFactory = new StdSchedulerFactory(props);

		this.scheduler = stdSchedulerFactory.getScheduler();
		this.housekeepingJobFactory = new HousekeepingJobFactory();
		this.scheduler.setJobFactory(housekeepingJobFactory);
	}

	public void registerManagedJob(ManagedHousekeepingJob managedJob) throws SchedulerException {
		log.info("Register new housekeeping job: {}", managedJob.getName());
		this.housekeepingJobFactory.registerJob(managedJob.getJobClass(), managedJob.getJobSupplier());

		// when housekeeping cron expression is changed, we want to reschedule a job
		SettingHook<ControllerSetting> hook = new SettingHook<>() {
			@Override
			public void onSettingSave(ControllerSetting setting) {
				try {
					log.info("Schedule the housekeeping job {} on controller setting save", managedJob.getName());
					scheduleHousekeepingJob(managedJob);
				} catch (Exception ex) {
					log.error("Cannot reschedule a housekeeping job. The controller setting won't be changed", ex);

					// this will cause the rollback in ControllerSettingAccessor - setting won't be changed
					throw new SettingHookRollbackException("Unable to schedule the housekeeping job", ex);
				}
			}

			@Override
			public void onSettingRemove(ObjectId id, ControllerSetting deleted) {
				try {
					log.info("Unschedule the housekeeping job {} on controller setting remove", managedJob.getName());
					unscheduleJob(managedJob);
				} catch (Exception ex) {
					log.error("Cannot unschedule a housekeeping job. The controller setting won't be changed");

					// this will cause the rollback in ControllerSettingAccessor - setting won't be changed
					throw new SettingHookRollbackException("Unable to unschedule the housekeeping job", ex);
				}
			}

		};
		String hookName = managedJob.getName();
		controllerSettingAccessor.addHook(hookName, hook);
		hooks.put(hookName, hook);
		scheduleHousekeepingJob(managedJob);
	}

	public void start() throws SchedulerException {
		log.info("Start housekeeping scheduler...");
		this.scheduler.start();
	}

	protected Scheduler getScheduler() {
		return scheduler;
	}

	/**
	 * Schedule a new housekeeping job in quartz or reschedule the existing job with actual cron schedule
	 */
	protected void scheduleHousekeepingJob(ManagedHousekeepingJob managedJob) throws SchedulerException {
		JobKey jobKey = managedJob.getJobKey();
		TriggerKey triggerKey = managedJob.getTriggerKey();

		String cronValue = controllerSettingAccessor.getSettingByKey(managedJob.getName()).getValue();

		log.info("Schedule a housekeeping job {} with cron value: {}", managedJob.getJobClass(), cronValue);

		CronScheduleBuilder schedBuilder = null;
		if (cronValue != null && !cronValue.isBlank()) {
			try {
				schedBuilder = CronScheduleBuilder.cronSchedule(cronValue);
			} catch (Exception ex) {
				// in case of misconfigured cron parameter we avoid a runtime exception
				throw new SchedulerException("Invalid cron schedule defined: " + cronValue, ex);
			}
		}

		// if we already have this job scheduled with this trigger, we will reschedule it
		unscheduleJob(managedJob);

		// we only can schedule a job if we have cron value defined in controller settings
		if (schedBuilder != null) {
			// define the job and tie it to our HelloJob class
			JobDetail job = JobBuilder.newJob(managedJob.getJobClass())
					.withIdentity(jobKey)
					.build();

			// Trigger the job to run at midnight by default
			Trigger trigger = newTrigger()
					.withIdentity(triggerKey)
					.withSchedule(schedBuilder)
					.build();

			// Tell quartz to schedule the job using our trigger
			scheduler.scheduleJob(job, trigger);
		}
	}

	private void unscheduleJob(ManagedHousekeepingJob job) throws SchedulerException {
		boolean reschedule = scheduler.checkExists(job.getJobKey());

		if (reschedule) {
			scheduler.unscheduleJob(job.getTriggerKey());
		}
	}

	public void shutdown() throws SchedulerException {
		for (Map.Entry<String, SettingHook> hookEntry : hooks.entrySet()) {
			controllerSettingAccessor.removeHook(hookEntry.getKey(), hookEntry.getValue());
		}
		scheduler.shutdown();
	}

	public static abstract class ManagedHousekeepingJob {
		protected abstract Class<? extends Job> getJobClass();
		protected abstract Supplier<? extends Job> getJobSupplier();
		protected abstract String getName();
		protected abstract TriggerKey getTriggerKey();
		protected abstract JobKey getJobKey();
	}

}
