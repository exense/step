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
import step.grid.agent.AgentTypes;
import step.grid.security.SymmetricSecurityConfiguration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Covers the configuration written for one agent run.
 * <p>
 * The settings written for every agent type are deliberately few: an agent is free to reject the settings it does not
 * implement, and the .NET agent does exactly that, refusing to start when it is given {@code ssl} or
 * {@code gridReadTimeout}. These tests pin which settings each agent type is given.
 */
public class AgentConfWriterTest {

    @Rule
    public final TemporaryFolder runDirectory = new TemporaryFolder();

    private final AgentConfWriter writer = new AgentConfWriter();

    @Test
    public void writesOnlyTheSettingsEveryAgentUnderstands() throws Exception {
        Map<String, Object> conf = write(Map.of());

        Assert.assertEquals("aGrid", conf.get("gridHost"));
        Assert.assertEquals(AgentConfWriter.LOOPBACK_HOST, conf.get("agentHost"));
        Assert.assertEquals("./work", conf.get("workingDir"));
        Assert.assertEquals(1000, conf.get("registrationPeriod"));
        // Not supported by every agent, and therefore written by the providers which need them
        Assert.assertFalse("ssl must not be written for every agent type", conf.containsKey("ssl"));
        Assert.assertFalse("gridReadTimeout must not be written for every agent type", conf.containsKey("gridReadTimeout"));
    }

    @Test
    public void writesTheTokensAndTheirAttributes() throws Exception {
        Map<String, Object> conf = write(Map.of());

        Map<String, Object> tokenGroup = ((java.util.List<Map<String, Object>>) conf.get("tokenGroups")).get(0);
        Assert.assertEquals(3, tokenGroup.get("capacity"));
        Map<String, Object> tokenConf = (Map<String, Object>) tokenGroup.get("tokenConf");
        Assert.assertEquals(Map.of(AgentTypes.AGENT_TYPE_KEY, "aType"), tokenConf.get("attributes"));
    }

    @Test
    public void writesTheSecretTheAgentsAuthenticateWith() throws Exception {
        Map<String, Object> conf = write(Map.of());

        Assert.assertEquals(Map.of("jwtSecretKey", "aSecret"), conf.get("gridSecurity"));
    }

    /**
     * The settings of one agent type are added to the ones written for every type.
     */
    @Test
    public void writesTheSettingsOfTheAgentType() throws Exception {
        Map<String, Object> conf = write(Map.of("workerName", "StepAgentWorker.exe", "agentPort", 12345));

        Assert.assertEquals("StepAgentWorker.exe", conf.get("workerName"));
        Assert.assertEquals(12345, conf.get("agentPort"));
        Assert.assertEquals("./work", conf.get("workingDir"));
    }

    /**
     * A provider cannot override what this writer guarantees, a mistake which would silently break the isolation of
     * the execution or the connection to the grid.
     */
    @Test
    public void doesNotLetAnAgentTypeOverrideTheSharedSettings() throws Exception {
        Map<String, Object> conf = write(Map.of("gridHost", "anotherGrid"));

        Assert.assertEquals("aGrid", conf.get("gridHost"));
    }

    private Map<String, Object> write(Map<String, Object> additionalSettings) throws IOException {
        SymmetricSecurityConfiguration security = new SymmetricSecurityConfiguration("aSecret");
        LocalAgentStartContext context = new LocalAgentStartContext("aGrid", runDirectory.getRoot().toPath(), 3,
            Map.of(AgentTypes.AGENT_TYPE_KEY, "aType"), security);

        Path confFile = writer.write(runDirectory.getRoot().toPath(), "AgentConf.yaml", context, additionalSettings);

        return new ObjectMapper(new YAMLFactory()).readValue(confFile.toFile(), Map.class);
    }
}
