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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.bson.types.ObjectId;
import org.everit.json.schema.ValidationException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.DefaultJacksonMapperProvider;
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

	private final ObjectMapper technicalPlanMapper;

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

		this.technicalPlanMapper = createTechnicalPlanObjectMapper();
	}

	protected ObjectMapper createTechnicalPlanObjectMapper(){
		YAMLFactory yamlFactory = new YAMLFactory();
		// Disable native type id to enable conversion to generic Documents
		yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
		return DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);
	}

	/**
	 * Writes the plan as technical YAML (full serialization)
	 */
	protected void writePlanInTechnicalFormat(OutputStream os, Plan plan) throws IOException {
		technicalPlanMapper.writeValue(os, plan);
	}

	@Test
	public void readPlanFromYaml() {
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/basic/test.plan.yml",
				"src/test/resources/step/plans/parser/yaml/basic/test-expected-tech-plan.yml"
		);
	}

	@Test
	public void keywordSelectionCriteria() {
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/selection-criteria/test-selection-criteria-plan.yml",
				"src/test/resources/step/plans/parser/yaml/selection-criteria/test-expected-selection-criteria-tech-plan.yml"
		);

		convertPlanToYaml("src/test/resources/step/plans/parser/yaml/selection-criteria/test-expected-selection-criteria-tech-plan.yml",
				"src/test/resources/step/plans/parser/yaml/selection-criteria/test-selection-criteria-converted-plan.yml"
		);
	}

	@Test
	public void checkArtefactExpression(){
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
	public void readInvalidYamlPlan(){
		// read simplified file
		File yamlFile = new File("src/test/resources/step/plans/parser/yaml/invalid/test-invalid-plan.yml");
		try (FileInputStream is = new FileInputStream(yamlFile)) {
			// convert yaml plan to technical format
			serializer.readYamlPlan(is);

			Assert.fail("Validation exception should be thrown");
		} catch (IOException e){
			throw new RuntimeException(e);
		} catch (ValidationException ex){
			log.info("OK - Validation exception caught", ex);
		}
	}

	@Test
	public void yamlPlanMigration(){
		convertFromYamlToPlan(
				"src/test/resources/step/plans/parser/yaml/migration/test-migration-plan.yml",
				"src/test/resources/step/plans/parser/yaml/migration/test-migration-expected-tech-plan.yml"
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
	public void checkConversionForBuildPlan(){
		// read plan
		File technicalPlanFile = new File("src/test/resources/step/plans/parser/yaml/build/test-build-tech-plan.yml");
		File yamlPlanFile = new File("src/test/resources/step/plans/parser/yaml/build/test-expected-build-plan.yml");
		File techYamlFileAfterConversion = new File("src/test/resources/step/plans/parser/yaml/build/test-expected-build-tech-converted-plan.yml");
		try (FileInputStream is = new FileInputStream(technicalPlanFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Plan plan = technicalPlanMapper.readValue(is, Plan.class);

			// convert plan to the yaml format
			serializer.writeYamlPlan(os, plan);
//			log.info("Converted yaml -->");
//			log.info(os.toString(StandardCharsets.UTF_8));

			// write yml to another file (to check it manually)
			if(writeResultsToLocalFiles) {
				try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/parser/yaml/test-expected-build-plan.yml")) {
					fileOs.write(os.toByteArray());
				}
			}

			JsonNode expectedYaml = serializer.getYamlMapper().readTree(yamlPlanFile);
			JsonNode actual = serializer.getYamlMapper().readTree(os.toByteArray());
			Assert.assertEquals(expectedYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// convert prepared yaml plan back to technical format
		try (FileInputStream is = new FileInputStream(yamlPlanFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Plan plan = serializer.readYamlPlan(is);
			writePlanInTechnicalFormat(os, plan);

//			log.info("Converted yaml -->");
//			log.info(os.toString(StandardCharsets.UTF_8));

			if (writeResultsToLocalFiles) {
				// write yml to another file (to check it manually)
				try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/parser/yaml/test-expected-build-tech-converted-plan.yml")) {
					fileOs.write(os.toByteArray());
				}
			}

			JsonNode expectedTechnicalYaml = technicalPlanMapper.readTree(techYamlFileAfterConversion);
			JsonNode actual = technicalPlanMapper.readTree(os.toByteArray());
			Assert.assertEquals(expectedTechnicalYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void convertPlanToYaml(String technicalPlanFilePath, String expectedYamlPlan) {
		// read plan
		File techYamlFile = new File(technicalPlanFilePath);

		try (FileInputStream is = new FileInputStream(techYamlFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Plan plan = technicalPlanMapper.readValue(is, Plan.class);

			// convert plan to the yaml format
			serializer.writeYamlPlan(os, plan);
			log.info("Converted yaml -->");
			log.info(os.toString(StandardCharsets.UTF_8));

			// write yml to another file (to check it manually)
			if(writeResultsToLocalFiles) {
				try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/parser/yaml/test-selection-criteria-converted-plan.yml")) {
					fileOs.write(os.toByteArray());
				}
			}

			JsonNode expectedYaml = serializer.getYamlMapper().readTree(new File(expectedYamlPlan));
			JsonNode actual = serializer.getYamlMapper().readTree(os.toByteArray());
			Assert.assertEquals(expectedYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void convertFromYamlToPlan(String yamlPlanFile, String expectedTechnialPlanFile) {
		// read yaml file
		File yamlFile = new File(yamlPlanFile);

		try (FileInputStream is = new FileInputStream(yamlFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			// convert yaml plan
			Plan plan = serializer.readYamlPlan(is);

			// serialize plan
			writePlanInTechnicalFormat(os, plan);
//			log.info("Converted technical plan -->");
//			log.info(os.toString(StandardCharsets.UTF_8));

			// write yml to another file (to check it manually)
			if (writeResultsToLocalFiles) {
				try (FileOutputStream fileOs = new FileOutputStream("src/test/resources/step/plans/parser/yaml/test-expected-tech-plan.yml")) {
					fileOs.write(os.toByteArray());
				}
			}

			// compare serialized plan with expected data
			JsonNode expectedTechnicalYaml = serializer.getYamlMapper().readTree(new File(expectedTechnialPlanFile));
			JsonNode actual = serializer.getYamlMapper().readTree(os.toByteArray());
			Assert.assertEquals(expectedTechnicalYaml, actual);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}