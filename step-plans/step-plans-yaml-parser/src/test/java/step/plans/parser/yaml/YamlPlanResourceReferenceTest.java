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
import org.junit.Test;
import step.artefacts.DataSetArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.datapool.excel.ExcelDataPool;
import step.plans.parser.yaml.model.YamlPlanVersions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * How the file of a data source is written when a plan is converted to yaml - which is the same
 * question for the automation package editor and for {@code GET /plans/&#123;id&#125;/yaml} on a
 * controller, and is answered in one place, {@code YamlResourceReference}.
 * <p>
 * A plan deployed from an automation package carries {@code apResource:<apId>:<path>}: the id of the
 * package it was read from, which belongs to that server and to no descriptor. Yaml gets the path.
 */
public class YamlPlanResourceReferenceTest {

    private final YamlPlanReader reader = new YamlPlanReader(YamlPlanVersions.ACTUAL_VERSION, true, null);

    @Test
    public void theResourceOfADeployedAutomationPackageIsWrittenAsARelativePath() throws IOException {
        assertEquals("\"data/my book.xlsx\"",
            workbookOfTheYamlOf("apResource:644fbe4e38a61e07cc3a4df9:data/my book.xlsx"));
    }

    @Test
    public void theResourceOfTheAutomationPackageBeingEditedIsWrittenAsARelativePath() throws IOException {
        assertEquals("\"data/my book.xlsx\"", workbookOfTheYamlOf("apResource:local:data/my book.xlsx"));
    }

    @Test
    public void aStepResourceIsStillWrittenAsAnId() throws IOException {
        assertEquals("{\"id\":\"644fbe4e38a61e07cc3a4df9\"}",
            workbookOfTheYamlOf("resource:644fbe4e38a61e07cc3a4df9"));
    }

    @Test
    public void aPathIsStillWrittenAsItIs() throws IOException {
        assertEquals("\"data/my book.xlsx\"", workbookOfTheYamlOf("data/my book.xlsx"));
    }

    /**
     * The plan of the editor is written through {@code planToYamlPlan} rather than
     * {@code writeYamlPlan} - a fragment of a descriptor carries no version header - so both entry
     * points are worth pinning.
     */
    @Test
    public void theTwoEntryPointsAgree() throws IOException {
        Plan plan = planReadingTheWorkbook("apResource:local:data/my book.xlsx");

        YamlPlan yamlPlan = reader.planToYamlPlan(plan);

        assertEquals("\"data/my book.xlsx\"", workbookOf(reader.getYamlMapper().valueToTree(yamlPlan)));
    }

    /**
     * The reference the entity holds is untouched: it is the one the execution resolves, and the plan
     * stays in memory after it has been written.
     */
    @Test
    public void writingThePlanDoesNotRewriteIt() throws IOException {
        Plan plan = planReadingTheWorkbook("apResource:local:data/my book.xlsx");

        workbookOfTheYamlOf(plan);

        assertEquals("apResource:local:data/my book.xlsx", workbookOfThePlan(plan));
    }

    private static Plan planReadingTheWorkbook(String reference) {
        ExcelDataPool dataPool = new ExcelDataPool();
        dataPool.setFile(new DynamicValue<>(reference));

        DataSetArtefact dataSet = new DataSetArtefact();
        dataSet.setDataSourceType("excel");
        dataSet.setDataSource(dataPool);

        Plan plan = new Plan(dataSet);
        YamlPlanReader.setPlanName(plan, "test plan");
        return plan;
    }

    private static String workbookOfThePlan(Plan plan) {
        return ((ExcelDataPool) ((DataSetArtefact) plan.getRoot()).getDataSource()).getFile().get();
    }

    private String workbookOfTheYamlOf(String reference) throws IOException {
        return workbookOfTheYamlOf(planReadingTheWorkbook(reference));
    }

    private String workbookOfTheYamlOf(Plan plan) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            reader.writeYamlPlan(os, plan);
            return workbookOf(reader.getYamlMapper().readTree(os.toByteArray()));
        }
    }

    private static String workbookOf(JsonNode yaml) {
        JsonNode dataSet = yaml.path("root").path("dataSet");
        assertTrue(yaml.toString(), dataSet.isObject());
        return dataSet.path("dataSource").path("excel").path("file").toString();
    }
}
