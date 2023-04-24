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
package step.core.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.plans.Plan;
import step.core.plans.serialization.YamlPlanSerializer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

// TODO: this test is in step-plans-base-artefact package, because we need all artifact classes loaded (see my-plan.yml)
public class YamlPlanSerializerTest {

	private static final Logger log = LoggerFactory.getLogger(YamlPlanSerializerTest.class);

	private final YamlPlanSerializer serializer = new YamlPlanSerializer();

	@Test
	public void readPlanFromYaml() {
		YAMLFactory factory = new YAMLFactory();
		ObjectMapper om = DefaultJacksonMapperProvider.getObjectMapper(factory);

		File yamlFile = new File("src/test/resources/step/core/plans/serialization/my-plan-short.yml");

		try (FileInputStream is = new FileInputStream(yamlFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Plan plan = serializer.readPlanFromYaml(is, "63dbe1052a5b7a70e3cbf9cd");

			serializer.toFullYaml(os, plan);

			JsonNode fullYamlResult = om.readTree(os.toByteArray());
			log.info(fullYamlResult.toPrettyString());

			JsonNode expectedFullYaml = om.readTree(new File("src/test/resources/step/core/plans/serialization/my-plan.yml"));
			Assert.assertEquals(expectedFullYaml, fullYamlResult);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}