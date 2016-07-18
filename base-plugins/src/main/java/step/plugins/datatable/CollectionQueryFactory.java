package step.plugins.datatable;

import javax.json.JsonObject;

public interface CollectionQueryFactory {

	public String buildAdditionalQuery(JsonObject filter);
}
