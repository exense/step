package step.core.scheduler.housekeeping;

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class HousekeepingJobFactory implements JobFactory {

    private final Map<Class<? extends Job>, Supplier<? extends Job>> jobSuppliers = new HashMap<>();

    public HousekeepingJobFactory() {
    }

    @Override
    public Job newJob(TriggerFiredBundle triggerFiredBundle, Scheduler scheduler) throws SchedulerException {
        Class<? extends Job> jobClass = triggerFiredBundle.getJobDetail().getJobClass();
        Supplier<? extends Job> jobSupplier = jobSuppliers.get(jobClass);
        if (jobSupplier == null) {
            throw new SchedulerException("No housekeeping job is registered for class: " + jobClass);
        } else {
            return jobSupplier.get();
        }
    }

    public void registerJob(Class<? extends Job> job, Supplier<? extends Job> supplier) {
        jobSuppliers.put(job, supplier);
    }

}
