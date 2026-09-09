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

import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

/**
 * Covers the settings a caller may only give a value the local execution can work with: a timeout no agent could meet
 * and an agent allowed no token are mistakes, and a shutdown timeout of zero additionally means "destroy it now".
 */
public class LocalAgentProvisioningConfigurationTest {

    private final LocalAgentProvisioningConfiguration configuration = new LocalAgentProvisioningConfiguration();

    @Test
    public void acceptsTheSmallestUsableTimeout() {
        configuration.setAgentShutdownTimeout(Duration.ofSeconds(1));
        configuration.setAgentStartTimeout(Duration.ofSeconds(1));

        Assert.assertEquals(Duration.ofSeconds(1), configuration.getAgentShutdownTimeout());
        Assert.assertEquals(Duration.ofSeconds(1), configuration.getAgentStartTimeout());
    }

    @Test
    public void rejectsAShutdownTimeoutOfZero() {
        IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
            () -> configuration.setAgentShutdownTimeout(Duration.ZERO));

        Assert.assertEquals("agentShutdownTimeout must be at least 1s, but was PT0S", exception.getMessage());
    }

    @Test
    public void rejectsAShutdownTimeoutShorterThanASecond() {
        Assert.assertThrows(IllegalArgumentException.class,
            () -> configuration.setAgentShutdownTimeout(Duration.ofMillis(999)));
    }

    @Test
    public void rejectsANegativeShutdownTimeout() {
        Assert.assertThrows(IllegalArgumentException.class,
            () -> configuration.setAgentShutdownTimeout(Duration.ofSeconds(-1)));
    }

    @Test
    public void rejectsANullShutdownTimeout() {
        Assert.assertThrows(NullPointerException.class, () -> configuration.setAgentShutdownTimeout(null));
    }

    @Test
    public void rejectsAStartTimeoutShorterThanASecond() {
        Assert.assertThrows(IllegalArgumentException.class, () -> configuration.setAgentStartTimeout(Duration.ZERO));
        Assert.assertThrows(IllegalArgumentException.class,
            () -> configuration.setAgentStartTimeout(Duration.ofMillis(999)));
    }

    @Test
    public void rejectsANullStartTimeout() {
        Assert.assertThrows(NullPointerException.class, () -> configuration.setAgentStartTimeout(null));
    }

    @Test
    public void acceptsASingleTokenPerAgent() {
        configuration.setMaxTokensPerAgent(1);

        Assert.assertEquals(1, configuration.getMaxTokensPerAgent());
    }

    @Test
    public void rejectsAnAgentAllowedNoToken() {
        IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
            () -> configuration.setMaxTokensPerAgent(0));

        Assert.assertEquals("maxTokensPerAgent must be at least 1, but was 0", exception.getMessage());
    }

    @Test
    public void rejectsANegativeNumberOfTokens() {
        Assert.assertThrows(IllegalArgumentException.class, () -> configuration.setMaxTokensPerAgent(-1));
    }
}
