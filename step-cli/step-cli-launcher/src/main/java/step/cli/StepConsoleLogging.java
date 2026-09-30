/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * What the {@code --verbose} and {@code --debug} options do to the logging of the CLI.
 * <p>
 * Applied to the logging configuration in place rather than through a configuration of its own, both options being
 * known only once the command line has been parsed.
 */
class StepConsoleLogging {

    /**
     * The conversion words of logback's exception converters, {@code %ex} and {@code %throwable} with their variants
     */
    private static final List<String> EXCEPTION_CONVERSION_WORDS = List.of("%ex", "%exception", "%throwable",
        "%xex", "%xexception", "%xthrowable", "%rex", "%rootexception", "%nopex", "%nopexception");

    /**
     * The loggers {@code --debug} raises to DEBUG: the Step packages, and only those.
     * <p>
     * Raising the root logger instead would also turn on the debug output of every library the CLI embeds, and Jetty
     * alone prints enough of it to bury what the developer asked for. A library which does need to be looked at can
     * still be turned up with a logging configuration of its own, passed with {@code -Dlogback.configurationFile}.
     * <p>
     * The agents of a local execution are configured the same way, in {@code logback-local-agent.xml}.
     */
    static final List<String> DEBUG_LOGGERS = List.of("step", "ch.exense");

    private StepConsoleLogging() {
    }

    /**
     * Applies both options, and nothing at all when the logging in place is not logback's.
     */
    static void apply(boolean debug, boolean verbose) {
        LoggerContext loggerContext = loggerContext();
        if (loggerContext == null) {
            return;
        }
        if (debug) {
            enableDebugLogging(loggerContext);
        }
        if (!verbose) {
            suppressStackTraces(loggerContext);
        }
    }

    private static LoggerContext loggerContext() {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        return loggerFactory instanceof LoggerContext ? (LoggerContext) loggerFactory : null;
    }

    /**
     * Raises {@link #DEBUG_LOGGERS} to DEBUG, leaving every other logger, and the root logger, at the level the
     * logging configuration gave them.
     */
    static void enableDebugLogging(LoggerContext loggerContext) {
        DEBUG_LOGGERS.forEach(name -> loggerContext.getLogger(name).setLevel(Level.DEBUG));
    }

    /**
     * Keeps the stack traces of the errors out of the output, which is what {@code --verbose} turns back on.
     * <p>
     * The errors the CLI reports are meant to be actionable on their own: a plan requiring an agent type this machine
     * has no installation of is a matter of the message, and the fifty frames printed under it bury it. This is done
     * on the appenders because most of these errors are logged by the execution engine, which the CLI shares with the
     * controller - where a stack trace in the log file is exactly what one wants.
     */
    static void suppressStackTraces(LoggerContext loggerContext) {
        loggerContext.getLoggerList()
            .forEach(logger -> logger.iteratorForAppenders().forEachRemaining(StepConsoleLogging::suppressStackTraces));
    }

    private static void suppressStackTraces(Appender<ILoggingEvent> appender) {
        if (!(appender instanceof OutputStreamAppender)) {
            return;
        }
        Encoder<ILoggingEvent> encoder = ((OutputStreamAppender<ILoggingEvent>) appender).getEncoder();
        if (!(encoder instanceof PatternLayoutEncoder)) {
            return;
        }
        PatternLayoutEncoder patternEncoder = (PatternLayoutEncoder) encoder;
        String pattern = patternEncoder.getPattern();
        // %nopex only suppresses the stack trace logback appends by itself, hence the check: a pattern placing the
        // exception explicitly says where it wants it, and is left alone. It also makes this idempotent.
        if (pattern == null || rendersTheException(pattern)) {
            return;
        }
        patternEncoder.stop();
        patternEncoder.setPattern(pattern + "%nopex");
        patternEncoder.start();
    }

    private static boolean rendersTheException(String pattern) {
        String lowerCasePattern = pattern.toLowerCase(Locale.ROOT);
        return EXCEPTION_CONVERSION_WORDS.stream().anyMatch(lowerCasePattern::contains);
    }
}
