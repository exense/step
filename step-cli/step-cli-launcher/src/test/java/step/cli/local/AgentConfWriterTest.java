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
package step.cli.local;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.grid.app.configuration.AppConfiguration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class AgentConfWriterTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    /**
     * The read timeout of an agent is also the read timeout of its file downloads, and a keyword's libraries travel
     * through those. Leaving it at the value an agent defaults to aborts the transfer of anything that takes longer,
     * which showed up as a stack trace on the grid and a silently retried download: see the AgentConf.yaml of the
     * agent distribution, which raises it for exactly this reason.
     */
    @Test
    public void raisesTheGridReadTimeoutAboveTheAgentDefault() throws Exception {
        Map<String, Object> conf = write(Map.of());

        Object gridReadTimeout = conf.get("gridReadTimeout");
        Assert.assertNotNull("The agent configuration should set a grid read timeout", gridReadTimeout);
        Assert.assertTrue("The grid read timeout should be raised above the agent default of "
                + new AppConfiguration().getGridReadTimeout() + "ms, was " + gridReadTimeout,
            (Integer) gridReadTimeout > new AppConfiguration().getGridReadTimeout());
    }

    @Test
    public void pinsTheAgentToTheLoopbackInterface() throws Exception {
        Map<String, Object> conf = write(Map.of());

        Assert.assertEquals(AgentConfWriter.LOOPBACK_HOST, conf.get("agentHost"));
    }

    @Test
    public void doesNotLetAnAgentTypeOverrideTheSettingsItShares() throws Exception {
        Map<String, Object> conf = write(Map.of("agentHost", "somewhere.else", "agentPort", 1234));

        Assert.assertEquals("An agent type must not be able to override a shared setting",
            AgentConfWriter.LOOPBACK_HOST, conf.get("agentHost"));
        Assert.assertEquals("Settings of its own must be written though", 1234, conf.get("agentPort"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> write(Map<String, Object> additionalSettings) throws IOException {
        Path directory = folder.getRoot().toPath();
        LocalAgentStartContext context = new LocalAgentStartContext("http://127.0.0.1:1234", directory, 2,
            Map.of("$agenttype", "default"), null);

        Path confFile = new AgentConfWriter().write(directory, "AgentConf.yaml", context, additionalSettings);

        return new ObjectMapper(new YAMLFactory()).readValue(confFile.toFile(), Map.class);
    }
}
