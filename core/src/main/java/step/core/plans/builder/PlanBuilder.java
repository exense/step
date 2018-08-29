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
		return new Plan(root, (Collection<AbstractArtefact>) localAccessor.getCollection());
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
		stack.pop();
		return this;
	}

	private void addToCurrentParent(AbstractArtefact artefact) {
		AbstractArtefact parent = stack.peek();
		localAccessor.get(parent.getId()).addChild(artefact.getId());
	}
}
