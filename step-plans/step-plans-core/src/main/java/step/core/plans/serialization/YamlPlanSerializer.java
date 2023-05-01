/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.plans.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.plans.Plan;
import step.core.plans.serialization.model.SimpleYamlPlan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class YamlPlanSerializer {

	private final ObjectMapper mapper;

	public YamlPlanSerializer() {
		YAMLFactory factory = new YAMLFactory();
		// Disable native type id to enable conversion to generic Documents
		factory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
		this.mapper = DefaultJacksonMapperProvider.getObjectMapper(factory);
	}

	public Plan readPlanFromYaml(InputStream inputStream) throws IOException {
		Plan plan = mapper.readValue(inputStream, Plan.class);
		return plan;
	}

	public SimpleYamlPlan readSimplePlanFromYaml(InputStream inputStream) throws IOException{
		// TODO: validate using the json schema?
		SimpleYamlPlan simpleYamlPlan = mapper.readValue(inputStream, SimpleYamlPlan.class);
		return simpleYamlPlan;
	}

	// TODO: make private
	public JsonNode convertSimplePlanToFullJson(SimpleYamlPlan simpleYamlPlan){
		ObjectNode top = mapper.createObjectNode();
		top.put(Plan.JSON_CLASS_FIELD, Plan.class.getName());
		ObjectNode planAttributesNode = mapper.createObjectNode();
		top.set("attributes", planAttributesNode);
		planAttributesNode.put("name", simpleYamlPlan.getName());

		top.set("root", convertSimpleArtifactToFull(simpleYamlPlan.getRoot()));
		return top;
	}

	public Plan convertSimplePlanToFullPlan(SimpleYamlPlan simpleYamlPlan) throws JsonProcessingException {
		JsonNode fullJson = convertSimplePlanToFullJson(simpleYamlPlan);
		Plan fullPlan = mapper.treeToValue(fullJson, Plan.class);
		// TODO: apply defaults
		return fullPlan;
	}

	private JsonNode convertSimpleArtifactToFull(JsonNode simpleArtifact) {

		List<String> specialFields = Arrays.asList("children", "name");

		String shortArtifactClass = simpleArtifact.fieldNames().next();
		JsonNode artifactData = simpleArtifact.get(shortArtifactClass);

		ObjectNode fullArtifact = mapper.createObjectNode();
		fullArtifact.put(Plan.JSON_CLASS_FIELD, shortArtifactClass);

		ObjectNode planAttributesNode = mapper.createObjectNode();
		planAttributesNode.put("name", artifactData.get("name").asText());
		fullArtifact.set("attributes", planAttributesNode);

		Iterator<Map.Entry<String, JsonNode>> fields = artifactData.fields();
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> next = fields.next();
			if (!specialFields.contains(next.getKey())) {
				fullArtifact.set(next.getKey(), next.getValue().deepCopy());
			}
		}

		JsonNode simpleChildren = artifactData.get("children");
		if (simpleChildren != null && simpleChildren.isArray()) {
			ArrayNode childrenResult = mapper.createArrayNode();
			for (JsonNode simpleChild : simpleChildren) {
				childrenResult.add(convertSimpleArtifactToFull(simpleChild));
			}
			fullArtifact.set("children", childrenResult);
		}

		return fullArtifact;
	}

	public void toFullYaml(OutputStream os, Plan plan) throws IOException {
		mapper.writeValue(os, plan);
	}

	public void toSimplifiedYaml(OutputStream os, SimpleYamlPlan plan) throws IOException {
		mapper.writeValue(os, plan);
	}
}
