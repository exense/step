package step.core.plans.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class YamlPlanJsonGenerator {

	private final JsonProvider jsonProvider = JsonProvider.provider();
	private final ObjectMapper objectMapper = new ObjectMapper();

	public JsonNode generateJsonSchema() throws JsonProcessingException {
		JsonObjectBuilder topLevelBuilder = jsonProvider.createObjectBuilder();
		topLevelBuilder.add("$schema", "https://json-schema.org/draft/2020-12/schema");
		topLevelBuilder.add("title", "Plan");
		topLevelBuilder.add("type", "object");
		topLevelBuilder.add("$defs", createDefs());
		topLevelBuilder.add("properties", createPlanProperties());
		topLevelBuilder.add("required", jsonProvider.createArrayBuilder().add("name").add("root"));

		return fromJakartaToJsonNode(topLevelBuilder);
	}

	private JsonObjectBuilder createPlanProperties() {
		JsonObjectBuilder objectBuilder = jsonProvider.createObjectBuilder();
		objectBuilder.add("name", jsonProvider.createObjectBuilder().add("type", "string"));
		objectBuilder.add("root", addRef(jsonProvider.createObjectBuilder(), "ArtefactDef"));
		return objectBuilder;
	}

	private JsonObjectBuilder addRef(JsonObjectBuilder builder, String refValue){
		return builder.add("$ref", "#/$defs/" + refValue);
	}

	private JsonObjectBuilder createDefs() {
		JsonObjectBuilder defsBuilder = jsonProvider.createObjectBuilder();

		Map<String, JsonObjectBuilder> dynamicValueDefs = createDynamicValueImplDefs();
		for (Map.Entry<String, JsonObjectBuilder> dynamicValueDef : dynamicValueDefs.entrySet()) {
			defsBuilder.add(dynamicValueDef.getKey(), dynamicValueDef.getValue());
		}

		Map<String, JsonObjectBuilder> artefactImplDefs = createArtefactImplDefs();
		for (Map.Entry<String, JsonObjectBuilder> artefactImplDef : artefactImplDefs.entrySet()) {
			defsBuilder.add(artefactImplDef.getKey(), artefactImplDef.getValue());
		}

		defsBuilder.add("ArtefactDef", createArtefactDef(artefactImplDefs.keySet()));
		return defsBuilder;
	}

	private Map<String, JsonObjectBuilder> createArtefactImplDefs() {
		Map<String, JsonObjectBuilder> res = new HashMap<>();
		// TODO: implement via reflection
		return res;
	}

	private Map<String, JsonObjectBuilder> createDynamicValueImplDefs() {
		Map<String, JsonObjectBuilder> res = new HashMap<>();
		res.put("DynamicValueStringDef", createDynamicValueDef("string"));
		res.put("DynamicValueNumDef", createDynamicValueDef("number"));
		res.put("DynamicValueBooleanDef", createDynamicValueDef("boolean"));
		return res;
	}

	private JsonObjectBuilder createDynamicValueDef(String valueType) {
		JsonObjectBuilder res = jsonProvider.createObjectBuilder();
		res.add("type", "object");
		JsonObjectBuilder properties = jsonProvider.createObjectBuilder();
		properties.add("expression", jsonProvider.createObjectBuilder().add("type", "string"));
		properties.add("expressionType", jsonProvider.createObjectBuilder().add("type", "string"));
		properties.add("value", jsonProvider.createObjectBuilder().add("type", valueType));
		res.add("properties", properties);
		return res;
	}

	private JsonObjectBuilder createArtefactDef(Collection<String> artefactImplReferences) {
		JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
		builder.add("type", "object");
		JsonArrayBuilder arrayBuilder = jsonProvider.createArrayBuilder();
		for (String artefactImplReference : artefactImplReferences) {
			arrayBuilder.add(addRef(jsonProvider.createObjectBuilder(), artefactImplReference));
		}
		builder.add("anyOf", arrayBuilder);
		return builder;
	}

	private JsonNode fromJakartaToJsonNode(JsonObjectBuilder objectBuilder) throws JsonProcessingException {
		return objectMapper.readTree(objectBuilder.build().toString());
	}

}
