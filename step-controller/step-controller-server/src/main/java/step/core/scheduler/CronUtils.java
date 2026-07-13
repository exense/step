package step.core.scheduler;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronExpression;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.calendar.BaseCalendar;
import org.quartz.impl.calendar.CronCalendar;
import org.quartz.spi.OperableTrigger;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

public final class CronUtils {

    private CronUtils() {
    }

    public static Long getNextExecutionDate(String cron, String excludedCron) throws ParseException {
        return getNextExecutionDate(cron, excludedCron == null ? null : List.of(excludedCron));
    }

    static Long getNextExecutionDate(String cron, List<String> excludedCrons) throws ParseException {
        if (excludedCrons == null || excludedCrons.isEmpty()) {
            return getNextExecutionDate(cron);
        }

        BaseCalendar calendar = new BaseCalendar();
        for (String excludedCron : excludedCrons) {
            calendar = new CronCalendar(calendar, excludedCron);
        }

        Trigger trigger = TriggerBuilder.newTrigger()
            .withSchedule(CronScheduleBuilder.cronSchedule(cron))
            .build();

        if (trigger instanceof OperableTrigger) {
            Date nextExecutionDate = ((OperableTrigger) trigger).computeFirstFireTime(calendar);
            return nextExecutionDate == null ? null : nextExecutionDate.getTime();
        } else {
            throw new RuntimeException("Unexpected trigger type: " + trigger.getClass().getName());
        }
    }

    private static Long getNextExecutionDate(String cron) throws ParseException {
        CronExpression cronExpression = new CronExpression(cron);
        Date nextExecutionDate = cronExpression.getNextValidTimeAfter(new Date());
        return nextExecutionDate == null ? null : nextExecutionDate.getTime();
    }
}
