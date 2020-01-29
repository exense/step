package step.core.plans;

import step.core.artefacts.AbstractArtefact;

public class PlanNavigator {

	protected final Plan plan;
	
	public PlanNavigator(Plan plan) {
		super();
		this.plan = plan;
	}

	public AbstractArtefact findArtefactById(String id) {
		return findArtefactByIdRecursive(id, plan.getRoot());
	}
	
	protected AbstractArtefact findArtefactByIdRecursive(String id, AbstractArtefact a) {
		if(a.getId().toString().equals(id)) {
			return a;
		} else {
			for (AbstractArtefact child : a.getChildren()) {
				AbstractArtefact result = findArtefactByIdRecursive(id, child);
				if(result != null) {
					return result;
				}
			}
			return null;
		}
	}
	
}
