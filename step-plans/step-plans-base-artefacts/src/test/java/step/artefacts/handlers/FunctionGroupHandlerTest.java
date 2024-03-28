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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.*;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.artefacts.handlers.functions.FunctionGroupSession;
import step.artefacts.handlers.functions.TokenAutoscalingExecutionPlugin;
import step.artefacts.handlers.functions.test.MyFunction;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.*;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.FunctionPlugin;
import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.execution.TokenLifecycleInterceptor;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.grid.AgentRef;
import step.grid.Token;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.tokenpool.Interest;
import step.planbuilder.FunctionArtefacts;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static step.artefacts.handlers.CallFunctionHandlerTest.newMyFunctionTypePlugin;

public class FunctionGroupHandlerTest {

    private TokenWrapper token;
    private TokenWrapper localToken;
    private AtomicBoolean localTokenReturned;
    private AtomicBoolean tokenReturned;

    @Before
    public void before() {
        token = token("remote");
        localToken = token("local");
        localTokenReturned = new AtomicBoolean(false);
        tokenReturned = new AtomicBoolean(false);
    }

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

    private void assertThatLocalAndRemoteTokenHaveBeenReleased() {
        assertTrue(localTokenReturned.get());
        assertTrue(tokenReturned.get());
    }

    private void markTokenAsReleased(String id) {
        if (localToken.getID().equals(id)) {
            localTokenReturned.set(true);
        }
        if (token.getID().equals(id)) {
            tokenReturned.set(true);
        }
    }

    private static void getLocalAndRemoteTokenFromSession(ExecutionContext t) {
        FunctionGroupContext functionGroupContext = (FunctionGroupContext) t.getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY);
        FunctionGroupSession session = functionGroupContext.getSession();
        getLocalAndRemoteToken(session);
        t.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
    }

    private static void getLocalAndRemoteToken(FunctionGroupSession session) {
        session.getLocalToken();
        try {
            session.getRemoteToken(Map.of(), null);
        } catch (FunctionExecutionServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private ExecutionEngine newEngineWithCustomTokenReleaseFunction(Consumer<String> tokenReleaseFunction) {
        return ExecutionEngine.builder().withOperationMode(OperationMode.CONTROLLER).withPlugin(new BaseArtefactPlugin()).withPlugin(new FunctionPlugin()).withPlugin(newMyFunctionTypePlugin()).withPlugin(new TokenAutoscalingExecutionPlugin()).withPlugin(new AbstractExecutionEnginePlugin() {

            @Override
            public void initializeExecutionContext(ExecutionEngineContext executionEngineContext,
                                                   ExecutionContext executionContext) {
                executionContext.put(FunctionExecutionService.class, new FunctionExecutionService() {

                    @Override
                    public void registerTokenLifecycleInterceptor(TokenLifecycleInterceptor interceptor) {

                    }

                    @Override
                    public void unregisterTokenLifecycleInterceptor(TokenLifecycleInterceptor interceptor) {

                    }

                    @Override
                    public TokenWrapper getLocalTokenHandle() {
                        return localToken;
                    }

                    @Override
                    public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests,
                                                       boolean createSession, TokenWrapperOwner tokenWrapperOwner) {
                        return token;
                    }

                    @Override
                    public void returnTokenHandle(String id) throws FunctionExecutionServiceException {
                        try {
                            tokenReleaseFunction.accept(id);
                        } catch (Exception e) {
                            throw new FunctionExecutionServiceException(e.getMessage());
                        }
                    }

                    @Override
                    public <IN, OUT> Output<OUT> callFunction(String id, Function function,
                                                              FunctionInput<IN> input, Class<OUT> outputClass) {
                        return (Output<OUT>) newOutput();
                    }

                    private Output<JsonObject> newOutput() {
                        Output<JsonObject> output = new Output<>();
                        JsonObject payload = Json.createObjectBuilder().build();
                        output.setPayload(payload);
                        output.setMeasures(new ArrayList<>());
                        output.setAttachments(new ArrayList<>());
                        return output;
                    }
                });
            }
        }).build();
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

        ExecutionEngine engine = newEngineWithCustomTokenReleaseFunction(id -> {
            if (localToken.getID().equals(id)) {
                localTokenReturned.incrementAndGet();
            }
            if (token.getID().equals(id)) {
                tokenReturned.incrementAndGet();
            }
        });

        StringWriter writer = new StringWriter();
        engine.execute(plan).printTree(writer);

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

    protected TokenWrapper token(String id) {
        Token localToken_ = new Token();
        localToken_.setId(id);
        localToken_.setAgentid(id);
        return new TokenWrapper(localToken_, new AgentRef());
    }

}
