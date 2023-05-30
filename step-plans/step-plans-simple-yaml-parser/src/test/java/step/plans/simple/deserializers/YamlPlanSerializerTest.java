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
package step.plans.simple.deserializers;

import com.fasterxml.jackson.databind.JsonNode;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.plans.Plan;
import step.plans.simple.YamlPlanSerializer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class YamlPlanSerializerTest {

	private static final Logger log = LoggerFactory.getLogger(YamlPlanSerializerTest.class);

	private static final ObjectId STATIC_ID = new ObjectId("644fbe4e38a61e07cc3a4df8") ;

	private final YamlPlanSerializer serializer = new YamlPlanSerializer(() -> STATIC_ID);

	@Test
	public void readSimplePlanFromYaml() {
		// read simplified file
		File yamlFile = new File("src/test/resources/step/plans/simple/test-simplified-plan.yml");

		try (FileInputStream is = new FileInputStream(yamlFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			// convert simplified plan to full plan
			Plan fullPlan = serializer.readSimplePlanFromYaml(is);

			// serialize plan to full yaml
			serializer.toFullYaml(os, fullPlan);
			log.info(os.toString(StandardCharsets.UTF_8));

			// compare serialized plan with expected data
			JsonNode expectedFullYaml = serializer.getMapper().readTree(new File("src/test/resources/step/plans/simple/test-full-expected-plan.yml"));
			JsonNode actual = serializer.getMapper().readTree(os.toByteArray());
			log.info("Converted plan: {}", actual.toPrettyString());
			Assert.assertEquals(expectedFullYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}