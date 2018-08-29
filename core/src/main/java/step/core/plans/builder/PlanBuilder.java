package step.core.plans.builder;

import java.util.Collection;
import java.util.Stack;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.InMemoryArtefactAccessor;
import step.core.plans.Plan;

public class PlanBuilder {

	protected AbstractArtefact root;

	protected InMemoryArtefactAccessor localAccessor = new InMemoryArtefactAccessor();
	
	protected Stack<AbstractArtefact> stack = new Stack<>();
	
	public static PlanBuilder create() {
		return new PlanBuilder();
	}
	
	@SuppressWarnings("unchecked")
	public Plan build() {
		if(stack.isEmpty()) {
			return new Plan(root, (Collection<AbstractArtefact>) localAccessor.getCollection());
		} else {
			throw new RuntimeException("Unbalanced block "+stack.peek());
		}
	}
	
	public PlanBuilder add(AbstractArtefact artefact) {
		if(root==null) {
			throw new RuntimeException("No root artefact defined. Please first call the method startBlock to define the root element");
		}
		localAccessor.save(artefact);
		addToCurrentParent(artefact);
		return this;
	}
	
	public PlanBuilder startBlock(AbstractArtefact a) {
		if(root!=null) {
			addToCurrentParent(a);
		} else {
			root = a;
		}
		localAccessor.save(a);
		stack.push(a);
		return this;
	}
	
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
