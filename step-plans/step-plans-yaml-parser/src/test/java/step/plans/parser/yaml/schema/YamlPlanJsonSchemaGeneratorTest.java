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
package step.plans.parser.yaml.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.plans.parser.yaml.model.YamlPlanVersions;

import java.io.IOException;
import java.io.InputStream;

public class YamlPlanJsonSchemaGeneratorTest {

	private static final Logger log = LoggerFactory.getLogger(YamlPlanJsonSchemaGeneratorTest.class);

	private final ObjectMapper jsonObjectMapper = new ObjectMapper();

	@Test
	public void generateJsonSchema() throws IOException, JsonSchemaPreparationException {
		log.info("Generating actual json schema for simplified plan format");

		// read published json schema
		InputStream jsonSchemaFile = this.getClass().getClassLoader().getResourceAsStream("step/plans/parser/yaml/step-yaml-plan-schema-os-1.0.json");

		JsonNode publishedSchema = jsonObjectMapper.readTree(jsonSchemaFile);
		YamlPlanJsonSchemaGenerator schemaGenerator = new YamlPlanJsonSchemaGenerator("step", YamlPlanVersions.ACTUAL_VERSION.getVersion());
		JsonNode currentSchema = schemaGenerator.generateJsonSchema();

		log.info("GENERATED SCHEMA:");
		log.info(currentSchema.toPrettyString());

		String errorMessage = "Published schema doesn't match to the actual one. To fix the test you need to publish " +
				"the generated schema printed above and actualize the published schema in current test";
		Assert.assertEquals(errorMessage, publishedSchema, currentSchema);
	}
}