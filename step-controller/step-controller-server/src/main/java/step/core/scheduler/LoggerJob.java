package step.core.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(LoggerJob.class);
    private Exception e;
    private String msg;

    public LoggerJob(String msg, Exception e) {
        this.e = e;
        this.msg = msg;
    }
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.error(msg, e);
    }
}
