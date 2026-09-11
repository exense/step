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
package step.agents.provisioning.local;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

/**
 * Covers the logging configuration given to the agents of a local execution. It is configured here and applied in the
 * agent process, so the levels it produces are worth pinning down where they can be read.
 */
public class LocalAgentLoggingConfigurationTest {

    private static final String CONFIGURATION_FILE = "logback-local-agent.xml";
    private static final String LOG_LEVEL_PROPERTY = "step.localAgent.logLevel";

    /**
     * What {@code --debug} gives: the debug output of the agent, without that of the libraries it embeds. Jetty at
     * debug level alone prints more than everything else together, and none of it is what the developer asked for.
     */
    @Test
    public void inDebugOnlyTheStepLoggersAreRaised() throws JoranException {
        LoggerContext loggerContext = configureWithLogLevel("debug");

        Assert.assertEquals(Level.DEBUG, loggerContext.getLogger("step.grid.agent.AgentServices").getEffectiveLevel());
        Assert.assertEquals(Level.DEBUG, loggerContext.getLogger("ch.exense.commons.io.FileHelper").getEffectiveLevel());
        Assert.assertEquals(Level.INFO, loggerContext.getLogger("org.eclipse.jetty.server.Server").getEffectiveLevel());
        Assert.assertEquals(Level.INFO, loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).getEffectiveLevel());
    }

    /**
     * The level the agents are started with when the CLI runs without {@code --debug}
     */
    @Test
    public void everythingLogsAtInfoByDefault() throws JoranException {
        LoggerContext loggerContext = configureWithLogLevel(null);

        Assert.assertEquals(Level.INFO, loggerContext.getLogger("step.grid.agent.AgentServices").getEffectiveLevel());
        Assert.assertEquals(Level.INFO, loggerContext.getLogger("org.eclipse.jetty.server.Server").getEffectiveLevel());
        Assert.assertEquals(Level.INFO, loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).getEffectiveLevel());
    }

    /**
     * @param logLevel the value the agent is started with, or null for an agent started without it
     */
    private static LoggerContext configureWithLogLevel(String logLevel) throws JoranException {
        URL configuration = LocalAgentLoggingConfigurationTest.class.getClassLoader().getResource(CONFIGURATION_FILE);
        Assert.assertNotNull("The logging configuration of the local agents is missing", configuration);

        // A context of its own: the one of this JVM is shared with every other test and must not be reconfigured here
        LoggerContext loggerContext = new LoggerContext();
        if (logLevel != null) {
            // The agent is given the level as a system property, which logback resolves the same way as this one
            loggerContext.putProperty(LOG_LEVEL_PROPERTY, logLevel);
        }
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        configurator.doConfigure(configuration);
        return loggerContext;
    }
}
