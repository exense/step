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
package step.plans.simple;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.bson.types.ObjectId;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.plans.serialization.model.SimpleYamlPlan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Supplier;

public class YamlPlanSerializer {

	private final ObjectMapper mapper;

	private final Supplier<ObjectId> idGenerator;

	// for tests
	public YamlPlanSerializer(Supplier<ObjectId> idGenerator) {
		YAMLFactory factory = new YAMLFactory();
		// Disable native type id to enable conversion to generic Documents
		factory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
		this.mapper = DefaultJacksonMapperProvider.getObjectMapper(factory);

		// configure custom deserializers
		SimpleModule module = new SimpleModule();
		module.addDeserializer(DynamicValue.class, new SimpleDynamicValueDeserializer());
		mapper.registerModule(module);

		this.idGenerator = idGenerator;
	}

	public YamlPlanSerializer() {
		this(null);
	}

	/**
	 * Read the plan from simplified yaml format
	 *
	 * @param inputStream yaml data
	 */
	public Plan readSimplePlanFromYaml(InputStream inputStream) throws IOException {
		// TODO: validate using the json schema?
		SimpleYamlPlan simplePlan = mapper.readValue(inputStream, SimpleYamlPlan.class);
		return convertSimplePlanToFullPlan(simplePlan);
	}

	public ObjectMapper getMapper() {
		return mapper;
	}

	private JsonNode convertSimplePlanToFullJson(SimpleYamlPlan simpleYamlPlan) {
		// apply 'simplified' structure - convert it to the common plan format
		ObjectNode top = mapper.createObjectNode();

		// _class field is omitted in simplified format
		top.put(Plan.JSON_CLASS_FIELD, Plan.class.getName());

		// the 'name' field is NOT wrapped into the 'attributes'
		ObjectNode planAttributesNode = mapper.createObjectNode();
		top.set("attributes", planAttributesNode);
		planAttributesNode.put("name", simpleYamlPlan.getName());

		// apply the simplified format for each artifact in tree
		top.set("root", convertSimpleArtifactToFull(simpleYamlPlan.getRoot()));
		return top;
	}

	private Plan convertSimplePlanToFullPlan(SimpleYamlPlan simpleYamlPlan) throws JsonProcessingException {
		JsonNode fullJson = convertSimplePlanToFullJson(simpleYamlPlan);
		Plan fullPlan = mapper.treeToValue(fullJson, Plan.class);

		applyDefaultValues(fullPlan);

		return fullPlan;
	}

	private void applyDefaultValues(Plan fullPlan) {
		// TODO: apply defaults
		if (this.idGenerator != null) {
			fullPlan.setId(this.idGenerator.get());
		}

		AbstractArtefact root = fullPlan.getRoot();
		if (root != null) {
			applyDefaultValuesForArtifact(root);
		}
	}

	private void applyDefaultValuesForArtifact(AbstractArtefact artifact) {
		if (this.idGenerator != null) {
			artifact.setId(this.idGenerator.get());
		}
		applyDefaultValuesForChildren(artifact);
	}

	private void applyDefaultValuesForChildren(AbstractArtefact root) {
		List<AbstractArtefact> children = root.getChildren();
		if (children != null) {
			for (AbstractArtefact child : children) {
				applyDefaultValuesForArtifact(child);
			}
		}
	}

	private JsonNode convertSimpleArtifactToFull(JsonNode simpleArtifact) throws JsonSchemaFieldProcessingException {
		ObjectNode fullArtifact = mapper.createObjectNode();

		// all fields except for 'children' and 'name' will be copied from simple artifact
		List<String> specialFields = Arrays.asList("children", "name");

		// move artifact class into the '_class' field
		Iterator<String> childrenArtifactNames = simpleArtifact.fieldNames();

		List<String> artifactNames = new ArrayList<String>();
		childrenArtifactNames.forEachRemaining(artifactNames::add);

		String shortArtifactClass = null;
		if (artifactNames.size() == 0) {
			throw new JsonSchemaFieldProcessingException("Artifact should have a name");
		} else if (artifactNames.size() > 1) {
			throw new JsonSchemaFieldProcessingException("Artifact should have only one name");
		} else {
			shortArtifactClass = artifactNames.get(0);
		}

		if (shortArtifactClass != null) {
			JsonNode artifactData = simpleArtifact.get(shortArtifactClass);
			fullArtifact.put(Plan.JSON_CLASS_FIELD, shortArtifactClass);

			// the 'name' field is NOT wrapped into the 'attributes'
			ObjectNode planAttributesNode = mapper.createObjectNode();

			// name is required attribute in json schema
			JsonNode name = artifactData.get("name");
			if (name == null) {
				throw new JsonSchemaFieldProcessingException("'name' attribute is not defined for artifact " + shortArtifactClass);
			}

			planAttributesNode.put("name", name.asText());
			fullArtifact.set("attributes", planAttributesNode);

			// copy all other fields (parameters)
			Iterator<Map.Entry<String, JsonNode>> fields = artifactData.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> next = fields.next();
				if (!specialFields.contains(next.getKey())) {
					fullArtifact.set(next.getKey(), next.getValue().deepCopy());
				}
			}

			// process children recursively
			JsonNode simpleChildren = artifactData.get("children");
			if (simpleChildren != null && simpleChildren.isArray()) {
				ArrayNode childrenResult = mapper.createArrayNode();
				for (JsonNode simpleChild : simpleChildren) {
					childrenResult.add(convertSimpleArtifactToFull(simpleChild));
				}
				fullArtifact.set("children", childrenResult);
			}
		}
		return fullArtifact;
	}

	/**
	 * Write the plan as YAML
	 */
	public void toFullYaml(OutputStream os, Plan plan) throws IOException {
		mapper.writeValue(os, plan);
	}

}
