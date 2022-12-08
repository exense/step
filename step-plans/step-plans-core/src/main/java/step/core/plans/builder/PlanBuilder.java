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
package step.core.plans.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;

/**
 * This class provides an API for the creation of {@link Plan}
 *
 */
public class PlanBuilder {

	protected AbstractArtefact root;

	protected Stack<AbstractArtefact> stack = new Stack<>();
	
	public static PlanBuilder create() {
		return new PlanBuilder();
	}
	
	/**
	 * @return the {@link Plan} created by this builder
	 */
	public Plan build() {
		if(stack.isEmpty()) {
			Plan plan = new Plan(root);
			plan.setAttributes(root.getName());
			return plan;
		} else {
			throw new RuntimeException("Unbalanced block "+stack.peek());
		}
	}
	
	/**
	 * Adds a node to the current parent
	 * @param artefact the {@link AbstractArtefact} to be added
	 * @return this instance of the {@link PlanBuilder}
	 */
	public PlanBuilder add(AbstractArtefact artefact) {
		if(root==null) {
			throw new RuntimeException("No root artefact defined. Please first call the method startBlock to define the root element");
		}
		addToCurrentParent(artefact);
		return this;
	}
	
	/**
	 * Adds a node to the current parent and defines it as the new current parent
	 * @param artefact the {@link AbstractArtefact} to be added and set as current parent
	 * @return this instance of the {@link PlanBuilder}
	 */
	public PlanBuilder startBlock(AbstractArtefact artefact) {
		if(root!=null) {
			addToCurrentParent(artefact);
		} else {
			root = artefact;
		}
		stack.push(artefact);
		return this;
	}
	
	/**
	 * Removes the current parent from the stack and switch back to the previous parent
	 * @return this instance of the {@link PlanBuilder}
	 */
	public PlanBuilder endBlock() {
		if(!stack.isEmpty()) {
			stack.pop();
		} else {
			throw new RuntimeException("Empty stack. Please first call startBlock before calling endBlock");
		}
		return this;
	}

	private void addToCurrentParent(AbstractArtefact artefact) {
		AbstractArtefact parent = stack.peek();
		parent.addChild(artefact);
	}
}
