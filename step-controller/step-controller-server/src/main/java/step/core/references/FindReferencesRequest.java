package step.core.references;

public class FindReferencesRequest {
    public enum Type {
        PLAN_ID,
        PLAN_NAME,
        KEYWORD_ID,
        KEYWORD_NAME
    }

    public Type referenceType;
    public String value;
    public boolean includeEphemerals;

    // Needed for Jax-WS
    public FindReferencesRequest() {
    }

    // Convenience constructor
    public FindReferencesRequest(Type referenceType, String value) {
        this.referenceType = referenceType;
        this.value = value;
    }
}
