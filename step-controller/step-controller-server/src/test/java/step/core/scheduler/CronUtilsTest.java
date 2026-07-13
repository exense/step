package step.core.scheduler;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;

public class CronUtilsTest {

    @Test
    public void testNextExecutionDateEveryHour() throws Exception {
        String everyHourCron = "0 0 * * * ?";

        long now = System.currentTimeMillis();
        Calendar nextRoundedHour = Calendar.getInstance();
        nextRoundedHour.setTimeInMillis(now);
        nextRoundedHour.set(Calendar.MINUTE, 0);
        nextRoundedHour.set(Calendar.SECOND, 0);
        nextRoundedHour.set(Calendar.MILLISECOND, 0);
        if (nextRoundedHour.getTimeInMillis() <= now) {
            nextRoundedHour.add(Calendar.HOUR_OF_DAY, 1);
        }

        Long nextExecutionDate = CronUtils.getNextExecutionDate(everyHourCron, Arrays.asList());

        assertEquals(nextRoundedHour.getTimeInMillis(), nextExecutionDate.longValue());
    }

    @Test
    public void testNextExecutionDateEveryHourWithNextHourExcluded() throws Exception {
        String everyHourCron = "0 0 * * * ?";

        long now = System.currentTimeMillis();
        Calendar nextRoundedHour = Calendar.getInstance();
        nextRoundedHour.setTimeInMillis(now);
        nextRoundedHour.set(Calendar.MINUTE, 0);
        nextRoundedHour.set(Calendar.SECOND, 0);
        nextRoundedHour.set(Calendar.MILLISECOND, 0);
        if (nextRoundedHour.getTimeInMillis() <= now) {
            nextRoundedHour.add(Calendar.HOUR_OF_DAY, 1);
        }

        String excludeNextRoundedHourCron = String.format("0 0 %d %d %d ? %d",
            nextRoundedHour.get(Calendar.HOUR_OF_DAY),
            nextRoundedHour.get(Calendar.DAY_OF_MONTH),
            nextRoundedHour.get(Calendar.MONTH) + 1,
            nextRoundedHour.get(Calendar.YEAR));

        Calendar expectedNextExecution = (Calendar) nextRoundedHour.clone();
        expectedNextExecution.add(Calendar.HOUR_OF_DAY, 1);

        Long nextExecutionDate = CronUtils.getNextExecutionDate(everyHourCron, Arrays.asList(excludeNextRoundedHourCron));

        assertEquals(expectedNextExecution.getTimeInMillis(), nextExecutionDate.longValue());
    }
}
