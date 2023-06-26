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
import org.bson.types.ObjectId;
import org.everit.json.schema.ValidationException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.plans.Plan;
import step.plans.simple.model.SimpleYamlPlanVersions;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class YamlPlanSerializerTest {

	private static final Logger log = LoggerFactory.getLogger(YamlPlanSerializerTest.class);

	private static final ObjectId STATIC_ID = new ObjectId("644fbe4e38a61e07cc3a4df8") ;

	private final YamlPlanSerializer serializer;

	public YamlPlanSerializerTest() throws IOException {
		String schemaFileLocation = "step/plans/simple/simplified-plan-schema-1.0.json";
		try(InputStream jsonSchemaInputStream = this.getClass().getClassLoader().getResourceAsStream(schemaFileLocation)){
			if(jsonSchemaInputStream == null){
				throw new IllegalStateException("Json schema not found: " + schemaFileLocation);
			}
			this.serializer = new YamlPlanSerializer(
					() -> STATIC_ID,
					SimpleYamlPlanVersions.ACTUAL_VERSION
			);
		}
	}

	@Test
	public void readSimplePlanFromYaml() {
		checkSimplePlanSerializationOk(
				"src/test/resources/step/plans/simple/basic/test-simple.plan.yml",
				"src/test/resources/step/plans/simple/basic/test-expected-full-plan.yml"
		);
	}

	@Test
	public void keywordSelectionCriteria() {
		checkSimplePlanSerializationOk(
				"src/test/resources/step/plans/simple/selection-criteria/test-selection-criteria-simple.plan.yml",
				"src/test/resources/step/plans/simple/selection-criteria/test-expected-selection-criteria-full-plan.yml"
		);
	}

	@Test
	public void readInvalidSimplePlanFromYaml(){
		// read simplified file
		File yamlFile = new File("src/test/resources/step/plans/simple/invalid/test-invalid-simple.plan.yml");
		try (FileInputStream is = new FileInputStream(yamlFile)) {
			// convert simplified plan to full plan
			serializer.readSimplePlanFromYaml(is);

			Assert.fail("Validation exception should be thrown");
		} catch (IOException e){
			throw new RuntimeException(e);
		} catch (ValidationException ex){
			log.info("OK - Validation exception caught", ex);
		}
	}

	@Test
	public void simplePlanMigration(){
		checkSimplePlanSerializationOk(
				"src/test/resources/step/plans/simple/migration/test-migration-simple.plan.yml",
				"src/test/resources/step/plans/simple/migration/test-migration-expected-full-plan.yml"
		);
	}

	@Test
	public void checkConversionFromFullPlanToSimple() {
		checkFullPlanConversionOk(
				"src/test/resources/step/plans/simple/basic/test-expected-full-plan.yml",
				"src/test/resources/step/plans/simple/basic/test-converted-from-full-simple.plan.yml"
		);
	}

	@Test
	public void checkConversionForBuildPlan(){
		// read full plan
		File fullYamlFile = new File("src/test/resources/step/plans/simple/build/test-build-full-plan.yml");
		File simpleYamlFile = new File("src/test/resources/step/plans/simple/build/test-expected-build-simple-plan.yml");
		File fullYamlFileAfterConversion = new File("src/test/resources/step/plans/simple/build/test-expected-build-full-converted-plan.yml");
		try (FileInputStream is = new FileInputStream(fullYamlFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Plan fullPlan = serializer.getFullYamlMapper().readValue(is, Plan.class);

			// convert full plan to the simple format
			serializer.writeSimpleYaml(os, serializer.convertFullPlanToSimplePlan(fullPlan));
//			log.info("Converted simple yaml -->");
//			log.info(os.toString(StandardCharsets.UTF_8));

			// write yml to another file (to check it manually)
			try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/simple/test-simple-generated-plan.yml")) {
				fileOs.write(os.toByteArray());
			}

			JsonNode expectedSimpleYaml = serializer.getSimpleYamlMapper().readTree(simpleYamlFile);
			JsonNode actual = serializer.getSimpleYamlMapper().readTree(os.toByteArray());
			Assert.assertEquals(expectedSimpleYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// convert prepared simple plan back to full format
		try (FileInputStream is = new FileInputStream(simpleYamlFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Plan fullPlan = serializer.readSimplePlanFromYaml(is);
			serializer.writeFullYaml(os, fullPlan);

//			log.info("Converted full yaml -->");
//			log.info(os.toString(StandardCharsets.UTF_8));

			// write yml to another file (to check it manually)
//			try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/simple/test-expected-build-full-converted-plan.yml")) {
//				fileOs.write(os.toByteArray());
//			}

			JsonNode expectedFullYaml = serializer.getFullYamlMapper().readTree(fullYamlFileAfterConversion);
			JsonNode actual = serializer.getFullYamlMapper().readTree(os.toByteArray());
			Assert.assertEquals(expectedFullYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkFullPlanConversionOk(String fullPlanFile, String expectedSimplePlan) {
		// read full plan
		File yamlFile = new File(fullPlanFile);

		try (FileInputStream is = new FileInputStream(yamlFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Plan fullPlan = serializer.getFullYamlMapper().readValue(is, Plan.class);

			// convert full plan to the simple format
			serializer.writeSimpleYaml(os, serializer.convertFullPlanToSimplePlan(fullPlan));
			log.info("Converted simple yaml -->");
			log.info(os.toString(StandardCharsets.UTF_8));

			// write yml to another file (to check it manually)
//			try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/simple/test-simple-generated-plan.yml")) {
//				fileOs.write(os.toByteArray());
//			}

			JsonNode expectedSimpleYaml = serializer.getSimpleYamlMapper().readTree(new File(expectedSimplePlan));
			JsonNode actual = serializer.getSimpleYamlMapper().readTree(os.toByteArray());
			Assert.assertEquals(expectedSimpleYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkSimplePlanSerializationOk(String simpleYamlPlanFile, String expectedFullPlanFile) {
		// read simplified file
		File yamlFile = new File(simpleYamlPlanFile);

		try (FileInputStream is = new FileInputStream(yamlFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			// convert simplified plan to full plan
			Plan fullPlan = serializer.readSimplePlanFromYaml(is);

			// serialize plan to full yaml
			serializer.writeFullYaml(os, fullPlan);
//			log.info("Converted full plan -->");
//			log.info(os.toString(StandardCharsets.UTF_8));

			// write yml to another file (to check it manually)
//			try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/simple/test-expected-build-full-converted-plan.yml")) {
//				fileOs.write(os.toByteArray());
//			}

			// compare serialized plan with expected data
			JsonNode expectedFullYaml = serializer.getSimpleYamlMapper().readTree(new File(expectedFullPlanFile));
			JsonNode actual = serializer.getSimpleYamlMapper().readTree(os.toByteArray());
			Assert.assertEquals(expectedFullYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}