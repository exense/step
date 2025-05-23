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
package step.plans.parser.yaml;

import step.core.yaml.model.NamedYamlArtefact;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import step.core.plans.agents.configuration.AgentProvisioningConfigurationDeserializer;
import step.core.plans.agents.configuration.AgentProvisioningConfigurationSerializer;
import step.core.plans.agents.configuration.AgentProvisioningConfiguration;

import java.util.List;

public class YamlPlan {

	public static final String PLANS_ENTITY_NAME = "plans";

	// this name should be kept untouched to support the migrations for old versions
	public static final String VERSION_FIELD_NAME = "version";

	private String version;
	private String name;

	private NamedYamlArtefact root;

	@JsonSerialize(using = AgentProvisioningConfigurationSerializer.class)
	@JsonDeserialize(using = AgentProvisioningConfigurationDeserializer.class)
	private AgentProvisioningConfiguration agents;

	private List<String> categories;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public NamedYamlArtefact getRoot() {
		return root;
	}

	public void setRoot(NamedYamlArtefact root) {
		this.root = root;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public AgentProvisioningConfiguration getAgents() {
		return agents;
	}

	public void setAgents(AgentProvisioningConfiguration agents) {
		this.agents = agents;
	}

	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}
}
