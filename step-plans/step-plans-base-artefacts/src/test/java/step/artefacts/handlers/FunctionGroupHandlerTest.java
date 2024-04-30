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
package step.artefacts.handlers;

import org.junit.Test;
import step.artefacts.*;
import step.artefacts.handlers.functions.test.MyFunction;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineException;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.planbuilder.FunctionArtefacts;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class FunctionGroupHandlerTest extends AbstractFunctionHandlerTest {

    @Test
    public void test() throws IOException, ExecutionEngineException {
        Plan plan = PlanBuilder.create().startBlock(new FunctionGroup()).add(new CheckArtefact(FunctionGroupHandlerTest::getLocalAndRemoteTokenFromSession)).add(new Echo()).endBlock().build();

        StringWriter writer = new StringWriter();
        try (ExecutionEngine engine = newEngineWithCustomTokenReleaseFunction(this::markTokenAsReleased)) {
            engine.execute(plan).printTree(writer);
        }

        // Assert that the token have been returned after Session execution
        assertThatLocalAndRemoteTokenHaveBeenReleased();
        assertEquals("Session:PASSED:\n" +
                " CheckArtefact:PASSED:\n" +
                " Echo:PASSED:\n", writer.toString());
    }

    @Test
    public void testReleaseMultipleErrors() throws IOException, ExecutionEngineException {
        Plan plan = PlanBuilder.create().startBlock(new FunctionGroup()).add(new CheckArtefact(t -> getLocalAndRemoteTokenFromSession(t)))
                .add(new Echo()).endBlock().build();

        StringWriter writer = new StringWriter();
        try (ExecutionEngine engine = newEngineWithCustomTokenReleaseFunction(id -> {
            markTokenAsReleased(id);
            throw new RuntimeException("Test error");
        })) {
            engine.execute(plan).printTree(writer);
        }

        // Assert that the token have been returned after Session execution
        assertThatLocalAndRemoteTokenHaveBeenReleased();
        assertEquals("Session:TECHNICAL_ERROR:Multiple errors occurred when releasing agent tokens: Test error, Test error\n" +
                " CheckArtefact:PASSED:\n" +
                " Echo:PASSED:\n", writer.toString());
    }

    @Test
    public void testReleaseErrors() throws IOException, ExecutionEngineException {
        Plan plan = PlanBuilder.create().startBlock(new FunctionGroup()).add(new CheckArtefact(t -> getLocalAndRemoteTokenFromSession(t))).add(new Echo()).endBlock().build();

        StringWriter writer = new StringWriter();
        try (ExecutionEngine engine = newEngineWithCustomTokenReleaseFunction(id -> {
            if (localToken.getID().equals(id)) {
                localTokenReturned.set(true);
            }
            if (token.getID().equals(id)) {
                tokenReturned.set(true);
                throw new RuntimeException("Test error");
            }
        })) {
            engine.execute(plan).printTree(writer);
        }

        // Assert that the token have been returned after Session execution
        assertThatLocalAndRemoteTokenHaveBeenReleased();
        assertEquals("Session:TECHNICAL_ERROR:Test error\n" +
                " CheckArtefact:PASSED:\n" +
                " Echo:PASSED:\n", writer.toString());
    }

    @Test
    public void testReleaseWaitingArtefacts() throws Exception {
        AtomicInteger localTokenReturned = new AtomicInteger();
        AtomicInteger tokenReturned = new AtomicInteger();

        Sleep sleepArtefact = new Sleep();
        sleepArtefact.setReleaseTokens(new DynamicValue<>(true));
        sleepArtefact.setDuration(new DynamicValue<>(100L));

        MyFunction function = new MyFunction(null);
        String name = UUID.randomUUID().toString();
        function.addAttribute(AbstractOrganizableObject.NAME, name);
        CallFunction callFunction = FunctionArtefacts.keyword(name);

        RetryIfFails retryIfFail = new RetryIfFails();
        retryIfFail.setReleaseTokens(new DynamicValue<>(true));
        retryIfFail.setMaxRetries(new DynamicValue<>(3));
        retryIfFail.setGracePeriod(new DynamicValue<>(200));
        Sequence sequence = new Sequence();
        sequence.setContinueOnError(new DynamicValue<>(true));
        sequence.addChild(sleepArtefact);
        sequence.addChild(callFunction);
        sequence.addChild(retryIfFail);
        sequence.addChild(callFunction);

        Plan plan = PlanBuilder.create().startBlock(new FunctionGroup()).add(new CheckArtefact(t -> getLocalAndRemoteTokenFromSession(t))).add(sequence).endBlock().build();
        plan.setFunctions(List.of(function));

        CheckArtefact check1 = new CheckArtefact(c -> c.getCurrentReportNode().setStatus(ReportNodeStatus.FAILED));
        retryIfFail.addChild(check1);

        StringWriter writer;
        try (ExecutionEngine engine = newEngineWithCustomTokenReleaseFunction(id -> {
            if (localToken.getID().equals(id)) {
                localTokenReturned.incrementAndGet();
            }
            if (token.getID().equals(id)) {
                tokenReturned.incrementAndGet();
            }
        })) {
            writer = new StringWriter();
            engine.execute(plan).printTree(writer);
        }

        // Assert that the token have been returned after Session execution
        assertEquals(1, localTokenReturned.get());
        assertEquals(3, tokenReturned.get());
        assertEquals(("Session:FAILED:\n" +
                " CheckArtefact:PASSED:\n" +
                " Sequence:FAILED:\n" +
                "  Sleep:PASSED:\n" +
                "  CallKeyword:PASSED:\n" +
                "  RetryIfFails:FAILED:\n" +
                "   Iteration1:FAILED:\n" +
                "    CheckArtefact:FAILED:\n" +
                "   Iteration2:FAILED:\n" +
                "    CheckArtefact:FAILED:\n" +
                "   Iteration3:FAILED:\n" +
                "    CheckArtefact:FAILED:\n" +
                "  CallKeyword:PASSED:\n").replace("CallKeyword", name), writer.toString());
    }

}
