package step.core.execution.model;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertEquals;
import static step.core.execution.model.ExecutionTimings.formatTimestamp;

public class ExecutionTimingsTests {

    // I just realized this has now really become a unit test for the built-in DateTimeFormatter with the default pattern, but let's keep it anyway...
    private static final DateTimeFormatter f = DateTimeFormatter.ofPattern(ExecutionTimings.DEFAULT_FORMATTER_PATTERN);

    @Test
    public void testFormatTimestamp_TrailingZeroPadding() {
        LocalDateTime base = LocalDateTime.of(2026, 9, 8, 13, 9, 54);
        ZoneOffset zulu = ZoneOffset.UTC;

        // .000 ms -> verifies zero-padding when millis are zero (i.e. 54.000, NOT 54)
        OffsetDateTime t1 = base.atOffset(zulu);
        assertEquals("2026-09-08T13:09:54.000+0000", formatTimestamp(f, t1));

        // .100 ms -> verifies trailing single zero is preserved (i.e. .100, NOT .1)
        OffsetDateTime t2 = base.plusNanos(100_000_000).atOffset(zulu);
        assertEquals("2026-09-08T13:09:54.100+0000", formatTimestamp(f, t2));

        // .740 ms -> verifies trailing zero in 3-digit millisecond value (i.e. .740, NOT .74)
        OffsetDateTime t3 = base.plusNanos(740_876_000).atOffset(zulu);
        assertEquals("2026-09-08T13:09:54.740+0000", formatTimestamp(f, t3));
    }

    @Test
    public void testFormatTimestamp_SubMillisecondTruncation() {
        LocalDateTime base = LocalDateTime.of(2026, 9, 8, 13, 9, 54);

        // 123 ms + 999,999 ns -> verifies truncation instead of rounding up to 124
        OffsetDateTime t = base.plusNanos(123_999_999).atOffset(ZoneOffset.UTC);
        assertEquals("2026-09-08T13:09:54.123+0000", formatTimestamp(f, t));
    }

    @Test
    public void testFormatTimestamp_TimezoneOffsets() {
        LocalDateTime base = LocalDateTime.of(2026, 9, 8, 13, 9, 54).plusNanos(123_456_789);

        // Zulu / UTC
        OffsetDateTime utc = base.atOffset(ZoneOffset.UTC);
        assertEquals("2026-09-08T13:09:54.123+0000", formatTimestamp(f, utc));

        // Positive offset (+02:00)
        OffsetDateTime positive = base.atOffset(ZoneOffset.ofHours(2));
        assertEquals("2026-09-08T13:09:54.123+0200", formatTimestamp(f, positive));

        // Negative offset (-05:00)
        OffsetDateTime negative = base.atOffset(ZoneOffset.ofHours(-5));
        assertEquals("2026-09-08T13:09:54.123-0500", formatTimestamp(f, negative));

        // Half-hour offset (+05:30)
        OffsetDateTime halfHour = base.atOffset(ZoneOffset.ofHoursMinutes(5, 30));
        assertEquals("2026-09-08T13:09:54.123+0530", formatTimestamp(f, halfHour));
    }
}
