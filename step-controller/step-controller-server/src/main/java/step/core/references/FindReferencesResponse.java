package step.core.references;

import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.functions.Function;

import java.util.Map;

public class FindReferencesResponse {

    // might be extended in the future, for now only plans are supported
    enum ReferrerType {
        PLAN,
        KEYWORD,
    }

    public final ReferrerType type;
    public final String id;
    public final String name;
    public final Map<String, String> attributes; 

    public FindReferencesResponse(ReferrerType type, String id, String name, Map<String, String> attributes) {
        this.type = type;
        this.id = id;
        this.name = name;
        this.attributes = attributes;
    }

    // convenience constructors
    public FindReferencesResponse(Plan plan) {
        this(ReferrerType.PLAN, plan.getId().toString(), plan.getAttribute(AbstractOrganizableObject.NAME), plan.getAttributes());
    }

    public FindReferencesResponse(Function function) {
        this(ReferrerType.KEYWORD, function.getId().toString(), function.getAttribute(AbstractOrganizableObject.NAME), function.getAttributes());
    }


}
