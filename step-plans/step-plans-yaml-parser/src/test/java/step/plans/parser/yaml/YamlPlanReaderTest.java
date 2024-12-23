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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.plans.Plan;
import step.plans.parser.yaml.schema.YamlPlanValidationException;
import step.repositories.parser.StepsParser;
import step.plans.parser.yaml.model.YamlPlanVersions;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class YamlPlanReaderTest {

	private static final Logger log = LoggerFactory.getLogger(YamlPlanReaderTest.class);

	private static final ObjectId STATIC_ID = new ObjectId("644fbe4e38a61e07cc3a4df8") ;

	private final YamlPlanReader yamlReader;

	// DEV flag to store test results in local files
	private boolean writeResultsToLocalFiles = false;

	private final ObjectMapper technicalPlanMapper;

	public YamlPlanReaderTest() {
		this.yamlReader = new YamlPlanReader(
				step.plans.parser.yaml.model.YamlPlanVersions.ACTUAL_VERSION,
				true,
				null
		);

		this.technicalPlanMapper = createTechnicalPlanObjectMapper();
	}

	protected ObjectMapper createTechnicalPlanObjectMapper(){
		YAMLFactory yamlFactory = new YAMLFactory();
		// Disable native type id to enable conversion to generic Documents
		yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
		ObjectMapper mapper = DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);
		// to avoid using null-values in comparison
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper;
	}

	/**
	 * Writes the plan as technical YAML (full serialization)
	 */
	protected void writePlanInTechnicalFormat(OutputStream os, Plan plan) throws IOException {
		technicalPlanMapper.writeValue(os, plan);
	}

	@Test
	public void readPlanFromYaml() throws YamlPlanValidationException {
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/basic/test.plan.yml",
				"src/test/resources/step/plans/parser/yaml/basic/test-expected-tech-plan.yml"
		);
	}

	@Test
	public void keywordSelectionCriteria() throws YamlPlanValidationException {
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/selection-criteria/test-selection-criteria-plan.yml",
				"src/test/resources/step/plans/parser/yaml/selection-criteria/test-expected-selection-criteria-tech-plan.yml"
		);

		convertPlanToYaml("src/test/resources/step/plans/parser/yaml/selection-criteria/test-expected-selection-criteria-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/selection-criteria/test-selection-criteria-converted-plan.yml"
		);
	}

	@Test
	public void functionGroup() throws YamlPlanValidationException {
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/function-group/test-function-group-plan.yml",
				"src/test/resources/step/plans/parser/yaml/function-group/test-expected-function-group-tech-plan.yml"
		);

		convertPlanToYaml("src/test/resources/step/plans/parser/yaml/function-group/test-expected-function-group-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/function-group/test-function-group-converted-plan.yml"
		);
	}

	@Test
	public void forBlock() throws YamlPlanValidationException {
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/for/test-for-plan.yml",
				"src/test/resources/step/plans/parser/yaml/for/test-expected-for-tech-plan.yml"
		);

		convertPlanToYaml("src/test/resources/step/plans/parser/yaml/for/test-expected-for-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/for/test-for-converted-plan.yml"
		);
	}

	@Test
	public void forEachBlock() throws YamlPlanValidationException {
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/for-each/test-for-each-plan.yml",
				"src/test/resources/step/plans/parser/yaml/for-each/test-expected-for-each-tech-plan.yml"
		);

		convertPlanToYaml("src/test/resources/step/plans/parser/yaml/for-each/test-expected-for-each-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/for-each/test-for-each-converted-plan.yml"
		);
	}

	@Test
	public void dataSet() throws YamlPlanValidationException {
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/data-set/test-data-set-plan.yml",
				"src/test/resources/step/plans/parser/yaml/data-set/test-expected-data-set-tech-plan.yml"
		);

		convertPlanToYaml("src/test/resources/step/plans/parser/yaml/data-set/test-expected-data-set-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/data-set/test-data-set-converted-plan.yml"
		);
	}

	@Test
	public void callPlan() throws YamlPlanValidationException {
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/call-plan/test-call-plan.yml",
				"src/test/resources/step/plans/parser/yaml/call-plan/test-expected-call-tech-plan.yml"
		);

		convertPlanToYaml("src/test/resources/step/plans/parser/yaml/call-plan/test-expected-call-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/call-plan/test-converted-from-tech-call-plan.yml"
		);
	}

	@Test
	public void testReturn() throws YamlPlanValidationException {
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/return/test-return-plan.yml",
				"src/test/resources/step/plans/parser/yaml/return/test-expected-return-tech-plan.yml"
		);

		convertPlanToYaml("src/test/resources/step/plans/parser/yaml/return/test-expected-return-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/return/test-return-converted-plan.yml"
		);
	}

	@Test
	public void readBenchmarkSampleYamlPlan() throws YamlPlanValidationException {
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/benchmark/test-benchmark-sample-plan.yml",
				"src/test/resources/step/plans/parser/yaml/benchmark/test-expected-benchmark-sample-tech-plan.yml"
		);

		convertPlanToYaml(
				"src/test/resources/step/plans/parser/yaml/benchmark/test-expected-benchmark-sample-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/benchmark/test-converted-benchmark-sample-plan.yml");
	}

	@Test
	public void checkArtefactExpression() throws YamlPlanValidationException {
		// test expressions in 'Check' artefact - convert from yaml format to technical format
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/check/test-check-plan.yml",
				"src/test/resources/step/plans/parser/yaml/check/test-expected-check-tech-plan.yml"
		);

		// convert from technical format to yaml format
		convertPlanToYaml(
				"src/test/resources/step/plans/parser/yaml/check/test-expected-check-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/check/test-converted-from-tech-check-plan.yml"
		);
	}

	@Test
	public void testAllControls() throws YamlPlanValidationException {
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/controls/test-controls-plan.yml",
				"src/test/resources/step/plans/parser/yaml/controls/test-expected-controls-tech-plan.yml"
		);

		convertPlanToYaml(
				"src/test/resources/step/plans/parser/yaml/controls/test-expected-controls-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/controls/test-controls-plan.yml"
		);
	}

	@Test
	public void readInvalidYamlPlan() throws IOException {
		// read simplified file
		File yamlFile = new File("src/test/resources/step/plans/parser/yaml/invalid/test-invalid-plan-1.yml");
		File yamlFile2 = new File("src/test/resources/step/plans/parser/yaml/invalid/test-invalid-plan-2.yml");
		try (FileInputStream is = new FileInputStream(yamlFile); FileInputStream is2 = new FileInputStream(yamlFile2)) {
			// convert yaml plan to technical format
			try {
				yamlReader.readYamlPlan(is);

				Assert.fail("Validation exception should be thrown");
			} catch (YamlPlanValidationException ex) {
				Assert.assertEquals("#: required key [name] not found", ex.getMessage());
				log.info("OK - Validation exception caught", ex);
			}

			try {
				yamlReader.readYamlPlan(is2);
				Assert.fail("Validation exception should be thrown");
			} catch (YamlPlanValidationException ex) {
				log.info("OK - Validation exception caught", ex);
			}
		}
	}

	@Test
	public void yamlPlanMigration() throws YamlPlanValidationException {
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/migration/test-migration-plan.yml",
				"src/test/resources/step/plans/parser/yaml/migration/test-migration-expected-tech-plan.yml"
		);
	}

	@Test
	public void testBeforeAndAfter() throws YamlPlanValidationException {
		//Test migration of older plans (BeforeSequence....)
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/beforeAfterMigration/test-before-after-old-version.yml",
				"src/test/resources/step/plans/parser/yaml/beforeAfterMigration/test-before-after-old-version-tech-plan.yml"
		);

		//Test new version

		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/beforeAfterMigration/test-before-after-plan.yml",
				"src/test/resources/step/plans/parser/yaml/beforeAfterMigration/test-before-after-tech-plan.yml"
		);

		convertPlanToYaml(
				"src/test/resources/step/plans/parser/yaml/controls/test-expected-controls-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/controls/test-controls-plan.yml"
		);
	}

	@Test
	public void checkConversionToYaml() {
		convertPlanToYaml(
				"src/test/resources/step/plans/parser/yaml/basic/test-expected-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/basic/test-converted-plan.yml"
		);
	}

	@Test
	public void checkConversionForBuildPlan() throws YamlPlanValidationException {
		// read plan
		File technicalPlanFile = new File("src/test/resources/step/plans/parser/yaml/build/test-build-tech-plan.yml");
		File yamlPlanFile = new File("src/test/resources/step/plans/parser/yaml/build/test-expected-build-plan.yml");
		File techYamlFileAfterConversion = new File("src/test/resources/step/plans/parser/yaml/build/test-expected-build-tech-converted-plan.yml");
		try (FileInputStream is = new FileInputStream(technicalPlanFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Plan plan = technicalPlanMapper.readValue(is, Plan.class);

			// convert plan to the yaml format
			yamlReader.writeYamlPlan(os, plan);
//			log.info("Converted yaml -->");
//			log.info(os.toString(StandardCharsets.UTF_8));

			// write yml to another file (to check it manually)
			if(writeResultsToLocalFiles) {
				try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/parser/yaml/test-expected-build-plan.yml")) {
					fileOs.write(os.toByteArray());
				}
			}

			JsonNode expectedYaml = yamlReader.getYamlMapper().readTree(yamlPlanFile);
			JsonNode actual = yamlReader.getYamlMapper().readTree(os.toByteArray());
			Assert.assertEquals(expectedYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// convert prepared yaml plan back to technical format
		try (FileInputStream is = new FileInputStream(yamlPlanFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Plan plan = yamlReader.readYamlPlan(is);
			writePlanInTechnicalFormat(os, plan);
			//Currently we use some workaround to overwrite all ids, but this only support artefact children (not the new properties: before, after, beforeThread...)
			//So we overwrite all ids here
			String outputWithStaticId = os.toString(StandardCharsets.UTF_8).replaceAll("(\"?id\"?: )\"[^\"]*\"", "$1\"" + STATIC_ID + "\"");
//			log.info("Converted yaml -->");
//			log.info(os.toString(StandardCharsets.UTF_8));

			if (writeResultsToLocalFiles) {
				// write yml to another file (to check it manually)
				try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/parser/yaml/test-expected-build-tech-converted-plan.yml")) {
					fileOs.write(outputWithStaticId.getBytes(StandardCharsets.UTF_8));
				}
			}

			JsonNode expectedTechnicalYaml = technicalPlanMapper.readTree(techYamlFileAfterConversion);
			JsonNode actual = technicalPlanMapper.readTree(outputWithStaticId.getBytes(StandardCharsets.UTF_8));
			Assert.assertEquals(expectedTechnicalYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void convertFromPlainTextToYaml() throws StepsParser.ParsingException {
		File plainTextPlan = new File("src/test/resources/step/plans/parser/yaml/plaintext/plaintext.plan");
		File expectedYamlFile = new File("src/test/resources/step/plans/parser/yaml/plaintext/plaintext-expected-plan.yml");

		try (FileInputStream is = new FileInputStream(plainTextPlan);
			 FileInputStream expectedIS = new FileInputStream(expectedYamlFile);
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			yamlReader.convertFromPlainTextToYaml("converted plaintext plan", is, os);

			if (writeResultsToLocalFiles) {
				// write yml to another file (to check it manually)
				try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/parser/yaml/test-converted-plan.yml")) {
					fileOs.write(os.toByteArray());
				}
			}
			String actualString = replaceDynamicValuesInActualTechOutput(os.toString(StandardCharsets.UTF_8));
			String expectedString = replaceDynamicValuesInExpectedInput(new String(expectedIS.readAllBytes(), StandardCharsets.UTF_8));
			JsonNode expectedTechnicalYaml = technicalPlanMapper.readTree(expectedString);
			JsonNode actual = technicalPlanMapper.readTree(actualString);
			Assert.assertEquals(expectedTechnicalYaml, actual);
		} catch (IOException ex){
			throw new RuntimeException(ex);
		}
	}

	@Test
	public void checkPlanYamlConfiguration() {
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/agents/test-agents-configuration-yaml.yml",
				"src/test/resources/step/plans/parser/yaml/agents/test-expected-agents-configuration-tech-plan.yml"
		);

		convertPlanToYaml(
				"src/test/resources/step/plans/parser/yaml/controls/test-expected-controls-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/controls/test-controls-plan.yml"
		);
	}

	private String replaceDynamicValuesInActualTechOutput(String input) {
		return input.replaceAll("id: \"[^\"]*\"", "id: \"" + STATIC_ID + "\"");

	}

	private String replaceDynamicValuesInExpectedInput(String input) {
		return input.replaceAll("(\n {0,2}version: )\"[^\"]*\"", "$1\"" + YamlPlanVersions.ACTUAL_VERSION + "\"");
	}

	private void convertPlanToYaml(String technicalPlanFilePath, String expectedYamlPlan) {
		// read plan
		File techYamlFile = new File(technicalPlanFilePath);

		try (FileInputStream is = new FileInputStream(techYamlFile);
			 FileInputStream expectedIS = new FileInputStream(expectedYamlPlan);
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Plan plan = technicalPlanMapper.readValue(is, Plan.class);

			// convert plan to the yaml format
			yamlReader.writeYamlPlan(os, plan);
			log.info("Converted yaml -->");
			log.info(os.toString(StandardCharsets.UTF_8));

			// write yml to another file (to check it manually)
			if(writeResultsToLocalFiles) {
				try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/parser/yaml/test-converted-plan.yml")) {
					fileOs.write(os.toByteArray());
				}
			}

			String expectedString = replaceDynamicValuesInExpectedInput(new String(expectedIS.readAllBytes(), StandardCharsets.UTF_8));
			JsonNode expectedYaml = yamlReader.getYamlMapper().readTree(expectedString);
			JsonNode actual = yamlReader.getYamlMapper().readTree(os.toByteArray());
			Assert.assertEquals(expectedYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void convertFromYamlToPlan(String yamlPlanFile, String expectedTechnicalPlanFile) throws YamlPlanValidationException {
		// read yaml file
		File yamlFile = new File(yamlPlanFile);

		try (FileInputStream is = new FileInputStream(yamlFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			// convert yaml plan
			Plan plan = yamlReader.readYamlPlan(is);

			// serialize plan
			writePlanInTechnicalFormat(os, plan);
			String actualTechString = replaceDynamicValuesInActualTechOutput(os.toString(StandardCharsets.UTF_8));
//			log.info("Converted technical plan -->");
//			log.info(os.toString(StandardCharsets.UTF_8));

			// write yml to another file (to check it manually)
			if (writeResultsToLocalFiles) {
				try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/parser/yaml/test-expected-tech-plan.yml")) {
					fileOs.write(actualTechString.getBytes(StandardCharsets.UTF_8));
				}
			}

			// compare serialized plan with expected data
			JsonNode expectedTechnicalYaml = yamlReader.getYamlMapper().readTree(new File(expectedTechnicalPlanFile));
			JsonNode actual = yamlReader.getYamlMapper().readTree(actualTechString);
			Assert.assertEquals(expectedTechnicalYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException("IO Exception", e);
		}
	}

}