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
package step.plans.parser.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import org.bson.types.ObjectId;
import org.everit.json.schema.ValidationException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.plans.Plan;
import step.plans.parser.yaml.model.YamlPlanVersions;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class YamlPlanReaderTest {

	private static final Logger log = LoggerFactory.getLogger(YamlPlanReaderTest.class);

	private static final ObjectId STATIC_ID = new ObjectId("644fbe4e38a61e07cc3a4df8") ;

	private final YamlPlanReader serializer;

	// DEV flag to store test results in local files
	private boolean writeResultsToLocalFiles = false;

	public YamlPlanReaderTest() throws IOException {
		String schemaFileLocation = "step/plans/parser/yaml/yaml-plan-schema-1.0.json";
		try(InputStream jsonSchemaInputStream = this.getClass().getClassLoader().getResourceAsStream(schemaFileLocation)){
			if(jsonSchemaInputStream == null){
				throw new IllegalStateException("Json schema not found: " + schemaFileLocation);
			}
			this.serializer = new YamlPlanReader(
					() -> STATIC_ID,
					YamlPlanVersions.ACTUAL_VERSION
			);
		}
	}

	@Test
	public void readSimplePlanFromYaml() {
		convertFromSimplePlanToFull(
				"src/test/resources/step/plans/parser/yaml/basic/test.plan.yml",
				"src/test/resources/step/plans/parser/yaml/basic/test-expected-tech-plan.yml"
		);
	}

	@Test
	public void keywordSelectionCriteria() {
		convertFromSimplePlanToFull(
				"src/test/resources/step/plans/parser/yaml/selection-criteria/test-selection-criteria-plan.yml",
				"src/test/resources/step/plans/parser/yaml/selection-criteria/test-expected-selection-criteria-tech-plan.yml"
		);
	}

	@Test
	public void checkArtefactExpression(){
		// test expressions in 'Check' artefact - convert from simple format to full format
		convertFromSimplePlanToFull(
				"src/test/resources/step/plans/parser/yaml/check/test-check-plan.yml",
				"src/test/resources/step/plans/parser/yaml/check/test-expected-check-tech-plan.yml"
		);

		// convert from full format to simple format
		convertFromFullPlanToSimple(
				"src/test/resources/step/plans/parser/yaml/check/test-expected-check-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/check/test-converted-from-tech-check-plan.yml"
		);
	}

	@Test
	public void readInvalidSimplePlanFromYaml(){
		// read simplified file
		File yamlFile = new File("src/test/resources/step/plans/parser/yaml/invalid/test-invalid-plan.yml");
		try (FileInputStream is = new FileInputStream(yamlFile)) {
			// convert simplified plan to full plan
			serializer.readYamlPlan(is);

			Assert.fail("Validation exception should be thrown");
		} catch (IOException e){
			throw new RuntimeException(e);
		} catch (ValidationException ex){
			log.info("OK - Validation exception caught", ex);
		}
	}

	@Test
	public void simplePlanMigration(){
		convertFromSimplePlanToFull(
				"src/test/resources/step/plans/parser/yaml/migration/test-migration-plan.yml",
				"src/test/resources/step/plans/parser/yaml/migration/test-migration-expected-tech-plan.yml"
		);
	}

	@Test
	public void checkConversionFromFullPlanToSimple() {
		convertFromFullPlanToSimple(
				"src/test/resources/step/plans/parser/yaml/basic/test-expected-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/basic/test-converted-plan.yml"
		);
	}

	@Test
	public void checkConversionForBuildPlan(){
		// read full plan
		File fullYamlFile = new File("src/test/resources/step/plans/parser/yaml/build/test-build-tech-plan.yml");
		File simpleYamlFile = new File("src/test/resources/step/plans/parser/yaml/build/test-expected-build-plan.yml");
		File fullYamlFileAfterConversion = new File("src/test/resources/step/plans/parser/yaml/build/test-expected-build-tech-converted-plan.yml");
		try (FileInputStream is = new FileInputStream(fullYamlFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Plan fullPlan = serializer.getTechnicalPlanMapper().readValue(is, Plan.class);

			// convert full plan to the simple format
			serializer.writeYamlPlan(os, fullPlan);
//			log.info("Converted simple yaml -->");
//			log.info(os.toString(StandardCharsets.UTF_8));

			// write yml to another file (to check it manually)
			if(writeResultsToLocalFiles) {
				try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/parser/yaml/test-simple-generated-plan.yml")) {
					fileOs.write(os.toByteArray());
				}
			}

			JsonNode expectedSimpleYaml = serializer.getYamlMapper().readTree(simpleYamlFile);
			JsonNode actual = serializer.getYamlMapper().readTree(os.toByteArray());
			Assert.assertEquals(expectedSimpleYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// convert prepared simple plan back to full format
		try (FileInputStream is = new FileInputStream(simpleYamlFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Plan fullPlan = serializer.readYamlPlan(is);
			serializer.writePlanInTechnicalFormat(os, fullPlan);

//			log.info("Converted full yaml -->");
//			log.info(os.toString(StandardCharsets.UTF_8));

			if (writeResultsToLocalFiles) {
				// write yml to another file (to check it manually)
				try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/parser/yaml/test-expected-build-tech-converted-plan.yml")) {
					fileOs.write(os.toByteArray());
				}
			}

			JsonNode expectedFullYaml = serializer.getTechnicalPlanMapper().readTree(fullYamlFileAfterConversion);
			JsonNode actual = serializer.getTechnicalPlanMapper().readTree(os.toByteArray());
			Assert.assertEquals(expectedFullYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void convertFromFullPlanToSimple(String fullPlanFile, String expectedSimplePlan) {
		// read full plan
		File yamlFile = new File(fullPlanFile);

		try (FileInputStream is = new FileInputStream(yamlFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Plan fullPlan = serializer.getTechnicalPlanMapper().readValue(is, Plan.class);

			// convert full plan to the simple format
			serializer.writeYamlPlan(os, fullPlan);
			log.info("Converted simple yaml -->");
			log.info(os.toString(StandardCharsets.UTF_8));

			// write yml to another file (to check it manually)
			if(writeResultsToLocalFiles) {
				try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/parser/yaml/test-simple-generated-plan.yml")) {
					fileOs.write(os.toByteArray());
				}
			}

			JsonNode expectedSimpleYaml = serializer.getYamlMapper().readTree(new File(expectedSimplePlan));
			JsonNode actual = serializer.getYamlMapper().readTree(os.toByteArray());
			Assert.assertEquals(expectedSimpleYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void convertFromSimplePlanToFull(String simpleYamlPlanFile, String expectedFullPlanFile) {
		// read simplified file
		File yamlFile = new File(simpleYamlPlanFile);

		try (FileInputStream is = new FileInputStream(yamlFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			// convert simplified plan to full plan
			Plan fullPlan = serializer.readYamlPlan(is);

			// serialize plan to full yaml
			serializer.writePlanInTechnicalFormat(os, fullPlan);
//			log.info("Converted full plan -->");
//			log.info(os.toString(StandardCharsets.UTF_8));

			// write yml to another file (to check it manually)
			if (writeResultsToLocalFiles) {
				try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/parser/yaml/test-expected-build-tech-converted-plan.yml")) {
					fileOs.write(os.toByteArray());
				}
			}

			// compare serialized plan with expected data
			JsonNode expectedFullYaml = serializer.getYamlMapper().readTree(new File(expectedFullPlanFile));
			JsonNode actual = serializer.getYamlMapper().readTree(os.toByteArray());
			Assert.assertEquals(expectedFullYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}