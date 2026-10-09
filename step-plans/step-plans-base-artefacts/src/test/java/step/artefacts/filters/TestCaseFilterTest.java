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
package step.artefacts.filters;

import org.junit.Test;
import step.artefacts.AbstractArtefactTest;
import step.core.artefacts.ArtefactFilter;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static step.planbuilder.BaseArtefacts.*;

public class TestCaseFilterTest extends AbstractArtefactTest {

    private static final String NESTED_PLAN_WITH_TC1_SELECTED = "Sequence:PASSED:\n" +
        " TC1:PASSED:\n" +
        "  Session:PASSED:\n" +
        "   TC1.1:PASSED:\n" +
        "    Session:PASSED:\n" +
        "     Echo:PASSED:\n" +
        "   Sequence:PASSED:\n" +
        "    TC1.2:PASSED:\n" +
        "     Session:PASSED:\n" +
        "      TC1.2.1:PASSED:\n" +
        "       Session:PASSED:\n" +
        "        Echo:PASSED:\n" +
        " TC2:SKIPPED:\n" +
        " Sequence:SKIPPED:\n" +
        "  TC3:SKIPPED:\n";

    @Test
    public void testNoFilter() throws IOException {
        assertEquals("Sequence:PASSED:\n" +
            " TC1:PASSED:\n" +
            "  Session:PASSED:\n" +
            "   Echo:PASSED:\n" +
            " TC2:PASSED:\n" +
            "  Session:PASSED:\n" +
            "   Echo:PASSED:\n", execute(flatPlan(), null));
    }

    @Test
    public void testFlatTestCases() throws IOException {
        assertEquals("Sequence:PASSED:\n" +
            " TC1:PASSED:\n" +
            "  Session:PASSED:\n" +
            "   Echo:PASSED:\n" +
            " TC2:SKIPPED:\n", execute(flatPlan(), new TestCaseFilter(List.of("TC1"))));
    }

    @Test
    public void testNestedTestCasesOfSelectedTestCaseAreExecuted() throws IOException {
        assertEquals(NESTED_PLAN_WITH_TC1_SELECTED, execute(nestedPlan(), new TestCaseFilter(List.of("TC1"))));
    }

    @Test
    public void testFilterIsReappliedAfterSelectedTestCase() throws IOException {
        // TC1.1 is a nested test case and is therefore not reachable when its parent isn't selected
        assertEquals("Sequence:PASSED:\n" +
            " TC1:SKIPPED:\n" +
            " TC2:PASSED:\n" +
            "  Session:PASSED:\n" +
            "   TC2.1:PASSED:\n" +
            "    Session:PASSED:\n" +
            "     Echo:PASSED:\n" +
            " Sequence:PASSED:\n" +
            "  TC3:PASSED:\n" +
            "   Session:PASSED:\n" +
            "    Echo:PASSED:\n", execute(nestedPlan(), new TestCaseFilter(List.of("TC2", "TC3", "TC1.1"))));
    }

    @Test
    public void testNestedTestCasesWithIdFilter() throws IOException {
        Plan plan = nestedPlan();
        String tc1Id = plan.getRoot().getChildren().get(0).getId().toString();
        assertEquals(NESTED_PLAN_WITH_TC1_SELECTED, execute(plan, new TestCaseIdFilter(List.of(tc1Id))));
    }

    private Plan flatPlan() {
        return PlanBuilder.create().startBlock(sequence())
            .startBlock(testCase("TC1")).add(echo("'TC1'")).endBlock()
            .startBlock(testCase("TC2")).add(echo("'TC2'")).endBlock()
            .endBlock().build();
    }

    /**
     * Sequence
     *  TC1
     *   TC1.1
     *   Sequence
     *    TC1.2
     *     TC1.2.1
     *  TC2
     *   TC2.1
     *  Sequence
     *   TC3
     */
    private Plan nestedPlan() {
        return PlanBuilder.create().startBlock(sequence())
            .startBlock(testCase("TC1"))
                .startBlock(testCase("TC1.1")).add(echo("'TC1.1'")).endBlock()
                .startBlock(sequence())
                    .startBlock(testCase("TC1.2"))
                        .startBlock(testCase("TC1.2.1")).add(echo("'TC1.2.1'")).endBlock()
                    .endBlock()
                .endBlock()
            .endBlock()
            .startBlock(testCase("TC2"))
                .startBlock(testCase("TC2.1")).add(echo("'TC2.1'")).endBlock()
            .endBlock()
            .startBlock(sequence())
                .startBlock(testCase("TC3")).add(echo("'TC3'")).endBlock()
            .endBlock()
            .endBlock().build();
    }

    private String execute(Plan plan, ArtefactFilter filter) throws IOException {
        ExecutionParameters executionParameters = new ExecutionParameters(plan, Map.of());
        executionParameters.setArtefactFilter(filter);
        StringWriter writer = new StringWriter();
        executionEngine.execute(executionParameters).printTree(writer);
        return writer.toString();
    }
}
