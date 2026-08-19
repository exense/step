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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes the configuration of a local agent.
 * <p>
 * The configuration is built here rather than being read from a template so that the values which matter for a local
 * execution are guaranteed to be set. It is serialized as a map instead of using the agents' own configuration
 * classes, which keeps the agent libraries off the CLI's classpath: the whole point of running the agents as separate
 * processes is that they bring their own, isolated set of libraries.
 * <p>
 * All agent types share the same configuration shape (grid host, token groups with a capacity and token attributes),
 * so the same writer serves the YAML of every agent type.
 * <p>
 * Only the settings <b>every</b> agent understands are written here; anything else is passed by the provider of the
 * agent type concerned. This is not mere tidiness: the .NET agent rejects the settings it does not implement rather
 * than ignoring them, and refuses to start when it is given, say, {@code ssl} or {@code gridReadTimeout}.
 */
public class AgentConfWriter {

    /**
     * The agents are only ever reached from this machine, over the loopback interface. The host is pinned rather than
     * left to the agent to determine, because agents fall back to resolving the canonical host name of the machine
     * (see {@code BaseServer.getOrBuildActualUrl}), which on a VPN, a multi-homed machine or one with a restricted DNS
     * resolves to a name the grid then fails to call back on.
     */
    public static final String LOOPBACK_HOST = "127.0.0.1";

    /**
     * How long an agent waits while reading from the grid, in milliseconds. Not written by this class: not every agent
     * supports it, see the class documentation.
     */
    public static final int GRID_READ_TIMEOUT_MS = 20000;

    private final ObjectMapper yamlMapper = new ObjectMapper(
        new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

    /**
     * Writes the agent configuration and returns the file it was written to.
     *
     * @param directory          the directory to write the configuration into
     * @param fileName           the name of the configuration file, e.g. {@code AgentConf.yaml}
     * @param context            the parameters of the agent to be started
     * @param additionalSettings agent type specific settings, merged into the configuration. Values already set by
     *                           this writer must not be overridden.
     */
    public Path write(Path directory, String fileName, LocalAgentStartContext context,
                      Map<String, Object> additionalSettings) throws IOException {
        Map<String, Object> conf = new LinkedHashMap<>();
        conf.put("gridHost", context.getGridUrl());
        conf.put("agentHost", LOOPBACK_HOST);
        // No agentPort: the agents bind to a free port chosen by the OS when none is configured. Reserving one here
        // would only add a race between the reservation and the agent actually binding it.
        conf.put("registrationPeriod", 1000);
        conf.put("workingDir", "./work");

        Map<String, Object> tokenConf = new LinkedHashMap<>();
        tokenConf.put("attributes", new LinkedHashMap<>(context.getTokenAttributes()));

        Map<String, Object> tokenGroup = new LinkedHashMap<>();
        tokenGroup.put("capacity", context.getNumberOfTokens());
        tokenGroup.put("tokenConf", tokenConf);
        conf.put("tokenGroups", List.of(tokenGroup));

        if (context.getGridSecurity() != null && context.getGridSecurity().isJwtAuthenticationEnabled()) {
            conf.put("gridSecurity", Map.of("jwtSecretKey", context.getGridSecurity().jwtSecretKey));
        }

        additionalSettings.forEach(conf::putIfAbsent);

        Path confFile = directory.resolve(fileName);
        Files.writeString(confFile, yamlMapper.writeValueAsString(conf));
        return confFile;
    }
}
