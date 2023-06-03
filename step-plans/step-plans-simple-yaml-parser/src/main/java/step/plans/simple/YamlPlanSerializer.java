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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.bson.types.ObjectId;
import step.artefacts.handlers.JsonSchemaValidator;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.plans.simple.deserializers.SimpleDynamicValueDeserializer;
import step.plans.simple.deserializers.SimpleRootArtefactDeserializer;
import step.plans.simple.model.SimpleRootArtefact;
import step.plans.simple.model.SimpleYamlPlan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;

public class YamlPlanSerializer {

	private final ObjectMapper yamlMapper;

	private final Supplier<ObjectId> idGenerator;
	private InputStream jsonSchemaFile = null;

	// for tests
	public YamlPlanSerializer(InputStream jsonSchemaFile, Supplier<ObjectId> idGenerator) {
		this.jsonSchemaFile = jsonSchemaFile;
		this.yamlMapper = createSimplePlanObjectMapper();
		this.idGenerator = idGenerator;
	}

	public static ObjectMapper createSimplePlanObjectMapper() {
		YAMLFactory yamlFactory = new YAMLFactory();
		// Disable native type id to enable conversion to generic Documents
		yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
		ObjectMapper yamlMapper = DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);

		// configure custom deserializers
		SimpleModule module = new SimpleModule();
		module.addDeserializer(DynamicValue.class, new SimpleDynamicValueDeserializer());
		module.addDeserializer(SimpleRootArtefact.class, new SimpleRootArtefactDeserializer());
		yamlMapper.registerModule(module);
		return yamlMapper;
	}

	public YamlPlanSerializer(InputStream jsonSchemaFile) {
		this(jsonSchemaFile, null);
	}

	/**
	 * Read the plan from simplified yaml format
	 *
	 * @param planYaml yaml data
	 */
	public Plan readSimplePlanFromYaml(InputStream planYaml) throws IOException {
		String bufferedYamlPlan = new String(planYaml.readAllBytes(), StandardCharsets.UTF_8);
		JsonNode simplePlanJsonNode = yamlMapper.readTree(bufferedYamlPlan);
		if (jsonSchemaFile != null) {
			String jsonSchema = new String(jsonSchemaFile.readAllBytes(), StandardCharsets.UTF_8);
			JsonSchemaValidator.validate(jsonSchema, simplePlanJsonNode.toString());
		}

		SimpleYamlPlan simplePlan = yamlMapper.treeToValue(simplePlanJsonNode, SimpleYamlPlan.class);
		return convertSimplePlanToFullPlan(simplePlan);
	}

	public ObjectMapper getYamlMapper() {
		return yamlMapper;
	}

	private Plan convertSimplePlanToFullPlan(SimpleYamlPlan simpleYamlPlan) {
		Plan fullPlan = new Plan(simpleYamlPlan.getRoot().getAbstractArtefact());
		fullPlan.addAttribute("name", simpleYamlPlan.getName());
		applyDefaultValues(fullPlan);
		return fullPlan;
	}

	private void applyDefaultValues(Plan fullPlan) {
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


	/**
	 * Write the plan as YAML
	 */
	public void toFullYaml(OutputStream os, Plan plan) throws IOException {
		yamlMapper.writeValue(os, plan);
	}

}
