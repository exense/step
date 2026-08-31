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
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Covers the logging a command applies to itself. These tests run on a logger context of their own: the one of the
 * test JVM is shared with every other test and must not be reconfigured here.
 */
public class StepConsoleLoggingTest {

    private final LoggerContext loggerContext = new LoggerContext();

    @Before
    public void setUp() {
        // A context built by hand has no MDC adapter, and every event appended to it would fail on it
        loggerContext.setMDCAdapter(new LogbackMDCAdapter());
    }

    /**
     * What a user sees without {@code --verbose}: the message of the error, and nothing of the stack trace, whether
     * the error was logged by the CLI or by the execution engine it runs the plans with.
     */
    @Test
    public void logsTheMessageOfAnErrorWithoutItsStackTrace() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Logger logger = rootLoggerWriting(output, "%msg%n");

        StepConsoleLogging.suppressStackTraces(loggerContext);
        logger.error("This plan requires agents which are not available for local execution",
            new IllegalStateException("no .NET agent installed"));

        String logged = output.toString(StandardCharsets.UTF_8);
        Assert.assertTrue("The message is kept: " + logged,
            logged.contains("This plan requires agents which are not available for local execution"));
        Assert.assertFalse("The stack trace is not printed: " + logged, logged.contains("IllegalStateException"));
        Assert.assertFalse("The stack trace is not printed: " + logged, logged.contains("\tat "));
    }

    /**
     * A pattern placing the exception itself says where it wants it, and is a deliberate choice of whoever configured
     * the logging.
     */
    @Test
    public void leavesAPatternWhichPlacesTheExceptionAlone() {
        PatternLayoutEncoder encoder = rootLoggerEncoder("%msg%n%xEx");

        StepConsoleLogging.suppressStackTraces(loggerContext);

        Assert.assertEquals("%msg%n%xEx", encoder.getPattern());
    }

    /**
     * Every command applies its logging options, and a command line can hold more than one.
     */
    @Test
    public void appliesTheSameSuppressionTwiceWithoutChangingThePatternTwice() {
        PatternLayoutEncoder encoder = rootLoggerEncoder("%msg%n");

        StepConsoleLogging.suppressStackTraces(loggerContext);
        String afterFirstCall = encoder.getPattern();
        StepConsoleLogging.suppressStackTraces(loggerContext);

        Assert.assertEquals(afterFirstCall, encoder.getPattern());
    }

    /**
     * The logging configuration of a distribution is not this CLI's to dictate: an appender it cannot rewrite is
     * skipped rather than reported.
     */
    @Test
    public void skipsAnAppenderWhichIsNotWrittenWithAPattern() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(loggerContext);
        appender.start();
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);

        StepConsoleLogging.suppressStackTraces(loggerContext);

        Assert.assertTrue(appender.isStarted());
    }

    /**
     * What {@code --debug} is for: the debug output of Step, in the CLI and in the code it shares with the
     * controller. The libraries stay where the logging configuration put them, or the lines the developer asked for
     * would be lost in those of Jetty.
     */
    @Test
    public void debugRaisesTheStepLoggersOnly() {
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.INFO);

        StepConsoleLogging.enableDebugLogging(loggerContext);

        Assert.assertEquals(Level.DEBUG, loggerContext.getLogger("step.cli.ApLocalExecuteCommandHandler").getEffectiveLevel());
        Assert.assertEquals(Level.DEBUG, loggerContext.getLogger("step.grid.filemanager.FileManagerImpl").getEffectiveLevel());
        Assert.assertEquals(Level.DEBUG, loggerContext.getLogger("ch.exense.commons.io.FileHelper").getEffectiveLevel());
        Assert.assertEquals(Level.INFO, loggerContext.getLogger("org.eclipse.jetty.server.Server").getEffectiveLevel());
        Assert.assertEquals(Level.INFO, loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).getEffectiveLevel());
    }

    private PatternLayoutEncoder rootLoggerEncoder(String pattern) {
        return (PatternLayoutEncoder) ((OutputStreamAppender<ILoggingEvent>) rootLoggerWriting(new ByteArrayOutputStream(), pattern)
            .getAppender("APPENDER")).getEncoder();
    }

    private Logger rootLoggerWriting(ByteArrayOutputStream output, String pattern) {
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern(pattern);
        encoder.start();

        OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<>();
        appender.setName("APPENDER");
        appender.setContext(loggerContext);
        appender.setEncoder(encoder);
        appender.setOutputStream(output);
        appender.start();

        Logger logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.addAppender(appender);
        return logger;
    }
}
