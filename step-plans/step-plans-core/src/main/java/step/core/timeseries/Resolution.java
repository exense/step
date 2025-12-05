package step.core.timeseries;

import java.time.Duration;
import java.util.Arrays;

/**
 * This enum define the time series resolutions supported in Step. It is critical to keep them ordered by resolution from the finest to the coarsest
 */
public enum Resolution {
    FIVE_SECONDS("5_seconds", Duration.ofSeconds(5), Duration.ofSeconds(1), false),
    FIFTEEN_SECONDS("15_seconds", Duration.ofSeconds(15), Duration.ofSeconds(5), false),
    ONE_MINUTE("minute", Duration.ofMinutes(1), Duration.ofSeconds(10), false),
    FIFTEEN_MINUTES("15_minutes", Duration.ofMinutes(15), Duration.ofMinutes(1), false),
    ONE_HOUR("hour", Duration.ofHours(1), Duration.ofMinutes(5), true),
    SIX_HOURS("6_hours", Duration.ofHours(6), Duration.ofMinutes(30), true),
    ONE_DAY("day", Duration.ofDays(1), Duration.ofHours(1), true),
    ONE_WEEK("week", Duration.ofDays(7), Duration.ofHours(2), true);

    /**
     * The name of the resolutions which is used to define the collections names as well as the Step properties and setting keys
      */
    public final String name;
    /**
     * The resolution duration
     */
    public final Duration resolution;
    /**
     * The default flush period if not configured in step.properties
     */
    public final Duration defaultFlushPeriod;
    /**
     * Whether this resolution is coarse, coarse resolution exclude specified attributes by the time-series creator (i.e. the execution id)
     */
    public final boolean coarseResolution;

    Resolution(String name, Duration resolution, Duration defaultFlushPeriod, boolean coarseResolution) {
        this.name = name;
        this.resolution = resolution;
        this.defaultFlushPeriod = defaultFlushPeriod;
        this.coarseResolution = coarseResolution;
    }

    public static Resolution fromName(String name) {
        return Arrays.stream(values())
                .filter(r -> r.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown resolution: " + name));
    }
}
