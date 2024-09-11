/*
 * Copyright (C) 2024, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

package step.core.plans.agents.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class AgentProvisioningConfigurationSerializer extends JsonSerializer<AgentProvisioningConfiguration> {
    @Override
    public void serialize(AgentProvisioningConfiguration agentProvisioningConfiguration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if(agentProvisioningConfiguration instanceof ManualAgentProvisioningConfiguration) {
            ManualAgentProvisioningConfiguration manualAgentProvisioningConfiguration = (ManualAgentProvisioningConfiguration) agentProvisioningConfiguration;
            jsonGenerator.writePOJO(manualAgentProvisioningConfiguration.configuredAgentPools);
        } else if (agentProvisioningConfiguration instanceof AutomaticAgentProvisioningConfiguration) {
            String mode = ((AutomaticAgentProvisioningConfiguration) agentProvisioningConfiguration).mode.toString();
            jsonGenerator.writeString(mode);
        } else {
            jsonGenerator.writePOJO(agentProvisioningConfiguration);
        }
    }
}
