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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.List;

public class AgentProvisioningConfigurationDeserializer extends JsonDeserializer {
    @Override
    public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonToken currentToken = jsonParser.currentToken();
        if (currentToken == JsonToken.START_ARRAY) {
            ManualAgentProvisioningConfiguration manualAgentProvisioningConfiguration = new ManualAgentProvisioningConfiguration();
            manualAgentProvisioningConfiguration.configuredAgentPools = jsonParser.readValueAs(new TypeReference<List<AgentPoolProvisioningConfiguration>>() {});
            return manualAgentProvisioningConfiguration;
        } else {
            return new AutomaticAgentProvisioningConfiguration(AutomaticAgentProvisioningConfiguration.PlanAgentsPoolAutoMode.valueOf(jsonParser.readValueAs(String.class)));
        }
    }
}
