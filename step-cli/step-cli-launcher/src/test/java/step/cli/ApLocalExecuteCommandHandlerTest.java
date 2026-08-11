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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.cli.local.LocalAgentProvisioningConfiguration;
import step.core.Constants;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

/**
 * Executes an automation package locally, end to end: the CLI deploys it into its embedded automation package
 * manager, extracts the Java agent embedded in its own jar, starts it as a separate process and runs the plans of the
 * package on it.
 * <p>
 * This is a smoke test of the whole path rather than an assertion on the plan results: the handler reports a failing
 * plan by logging it, not by throwing, so what is checked here is that deploying the package, provisioning the agent
 * and running every plan on it completes without error.
 */
public class ApLocalExecuteCommandHandlerTest {

    @Rule
    public final TemporaryFolder workDirectory = new TemporaryFolder();

    @Test
    public void executesAnAutomationPackageOnALocalAgent() throws Exception {
        File automationPackage = new File("src/test/resources/samples/step-automation-packages-sample1.jar");

        LocalAgentProvisioningConfiguration configuration = new LocalAgentProvisioningConfiguration()
            .setWorkDirectory(workDirectory.getRoot().toPath());

        new ApLocalExecuteCommandHandler()
            .execute(automationPackage, null, null, null, null, null, Map.of(), configuration);

        // The handler logs plan failures instead of throwing, so completing without an exception proves very little
        // on its own. The extracted agent is the evidence that the sample's Java keyword really was provisioned onto
        // a forked agent, rather than the whole local execution having silently degraded to "no agent available".
        Assert.assertTrue("The Java agent should have been extracted and started",
            Files.isRegularFile(workDirectory.getRoot().toPath()
                .resolve("agents").resolve("java").resolve(Constants.STEP_VERSION_STRING).resolve("step-agent.jar")));
    }
}
