package step.core.references;

public class FindReferencesRequest {
    public enum Type {
        PLAN_ID,
        PLAN_NAME,
        KEYWORD_ID,
        KEYWORD_NAME
    }

    public Type searchType;
    public String searchValue;
    public boolean includeEphemerals;

    // Needed for Jax-WS
    public FindReferencesRequest() {
    }

    // Convenience constructor
    public FindReferencesRequest(Type searchType, String searchValue) {
        this.searchType = searchType;
        this.searchValue = searchValue;
    }
}
