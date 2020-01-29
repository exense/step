package step.core.plans;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.functions.Function;

@JsonTypeInfo(use=Id.NAME,property="_class")
public class Plan extends AbstractOrganizableObject {

	protected AbstractArtefact root;
	
	protected Collection<Function> functions;
	
	protected Collection<Plan> subPlans;
	
	public Plan(AbstractArtefact root) {
		super();
		this.root = root;
	}

	public Plan() {
		super();
	}

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
}
