package step.plugins.streaming.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * This class exists because when setting (numeric) variables in Groovy, we can't always predict the type
 * that it will use for storing the value. This class can handle pretty much any type that we might encounter,
 * and convert it to the appropriate type. As a bonus, it also handles string values, which allows to use
 * underscores in large numbers (e.g. 100_000_000).
 */
public final class NumberCoercions {
    private NumberCoercions() {}

    /**
     * Convert a dynamic Groovy/Java value to a Long.
     * Strict rules:
     *  - Only integral values are accepted (no fractional part).
     *  - Must fit in the signed 64-bit range.
     *  - Strings/GStrings are parsed in base 10 (underscores allowed).
     *
     * @throws IllegalArgumentException if the value cannot be converted exactly.
     */
    public static Long asLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return ((Number) value).longValue();
        }
        if (value instanceof BigInteger) {
            return ((BigInteger) value).longValueExact(); // throws if out of range
        }
        if (value instanceof BigDecimal) {
            // throws if there is a fractional part or out of long range
            return ((BigDecimal) value).longValueExact();
        }
        if (value instanceof Number) {
            // Covers Double/Float/etc. Accept only if integral and in range.
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("Non-finite number: " + value);
            }
            long l = (long) d;
            if (d != (double) l) {
                throw new IllegalArgumentException("Non-integer numeric value: " + value);
            }
            return l; // already within long range if cast succeeded losslessly
        }
        if (value instanceof CharSequence) { // also catches Groovy's GString
            String s = value.toString().trim().replace("_", "");
            if (s.isEmpty()) {
                throw new IllegalArgumentException("Empty numeric string");
            }
            try {
                // Try integer first (no decimals)
                return new BigInteger(s).longValueExact();
            } catch (NumberFormatException | ArithmeticException intEx) {
                // Fall back to BigDecimal to allow e.g. "42.0" only if exactly integral
                try {
                    return new BigDecimal(s).longValueExact();
                } catch (NumberFormatException | ArithmeticException decEx) {
                    throw new IllegalArgumentException("Not a valid integral number: " + value);
                }
            }
        }

        throw new IllegalArgumentException("Unsupported type for long conversion: " + value.getClass().getName());
    }
}
