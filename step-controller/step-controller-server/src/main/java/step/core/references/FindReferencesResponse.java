package step.core.references;

import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;

public class FindReferencesResponse {

    // might be extended in the future, for now only plans are supported
    enum ReferrerType {
        PLAN,
    }

    public final ReferrerType type;
    public final String id;
    public final String name;
    public final String projectId;

    public FindReferencesResponse(ReferrerType type, String id, String name, String projectId) {
        this.type = type;
        this.id = id;
        this.name = name;
        this.projectId = projectId;
    }

    // convenience constructor
    public FindReferencesResponse(Plan plan) {
        this(ReferrerType.PLAN, plan.getId().toString(), plan.getAttribute(AbstractOrganizableObject.NAME), plan.getAttribute("project"));
    }

}
