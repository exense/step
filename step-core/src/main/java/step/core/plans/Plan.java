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
package step.core.plans;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import step.commons.activation.Expression;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.entities.EntityConstants;
import step.core.entities.EntityReference;
import step.core.objectenricher.EnricheableObject;
import step.core.plans.agents.configuration.AgentProvisioningConfiguration;
import step.core.plans.agents.configuration.AutomaticAgentProvisioningConfiguration;
import step.functions.EvaluationExpression;
import step.functions.Function;

@JsonTypeInfo(use=Id.CLASS,property= Plan.JSON_CLASS_FIELD)
public class Plan extends AbstractOrganizableObject implements EnricheableObject, EvaluationExpression {

	public static final String JSON_CLASS_FIELD = "_class";

	protected AbstractArtefact root;
	
	protected Collection<Function> functions;
	
	protected Collection<Plan> subPlans;

	@JsonTypeInfo(use= Id.DEDUCTION)
	protected AgentProvisioningConfiguration agents = new AutomaticAgentProvisioningConfiguration(AutomaticAgentProvisioningConfiguration.PlanAgentsPoolAutoMode.auto_detect);
	
	protected boolean visible = true;

	protected Expression activationExpression;

	private List<String> categories;
	
	public Plan(AbstractArtefact root) {
		super();
		this.root = root;
	}

	public Plan() {
		super();
	}

	@EntityReference(type= EntityConstants.recursive)
	public AbstractArtefact getRoot() {
		return root;
	}

	public void setRoot(AbstractArtefact root) {
		this.root = root;
	}
	
	public Collection<Function> getFunctions() {
		return functions;
	}

	public void setFunctions(Collection<Function> functions) {
		this.functions = functions;
	}

	public Collection<Plan> getSubPlans() {
		return subPlans;
	}

	public void setSubPlans(Collection<Plan> subPlans) {
		this.subPlans = subPlans;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
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

	@Override
	public Expression getActivationExpression() {
		return activationExpression;
	}

	public void setActivationExpression(Expression activationExpression) {
		this.activationExpression = activationExpression;
	}
}
