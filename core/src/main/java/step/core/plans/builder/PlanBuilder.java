package step.core.plans.builder;

import java.util.Collection;
import java.util.Stack;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.InMemoryArtefactAccessor;
import step.core.plans.Plan;

/**
 * This class provides an API for the creation of {@link Plan}
 *
 */
public class PlanBuilder {

	protected AbstractArtefact root;

	protected InMemoryArtefactAccessor localAccessor = new InMemoryArtefactAccessor();
	
	protected Stack<AbstractArtefact> stack = new Stack<>();
	
	public static PlanBuilder create() {
		return new PlanBuilder();
	}
	
	/**
	 * @return the {@link Plan} created by this builder
	 */
	@SuppressWarnings("unchecked")
	public Plan build() {
		if(stack.isEmpty()) {
			return new Plan(root, (Collection<AbstractArtefact>) localAccessor.getCollection());
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
		localAccessor.save(artefact);
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
		localAccessor.save(artefact);
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
		localAccessor.get(parent.getId()).addChild(artefact.getId());
	}
}
