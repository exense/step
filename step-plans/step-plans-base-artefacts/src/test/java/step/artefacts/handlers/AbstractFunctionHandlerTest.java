package step.artefacts.handlers;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.Before;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.handlers.functions.FunctionGroupSession;
import step.artefacts.handlers.functions.TokenAutoscalingExecutionPlugin;
import step.artefacts.handlers.functions.test.MyFunctionType;
import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.FunctionPlugin;
import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.execution.TokenLifecycleInterceptor;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.functions.type.FunctionTypeRegistry;
import step.grid.AgentRef;
import step.grid.Token;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.tokenpool.Interest;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;

public class AbstractFunctionHandlerTest {

    protected TokenWrapper token;
    protected TokenWrapper localToken;
    protected AtomicBoolean localTokenReturned;
    protected AtomicBoolean tokenReturned;

    @Before
    public void before() {
        token = token("remote");
        localToken = token("local");
        localTokenReturned = new AtomicBoolean(false);
        tokenReturned = new AtomicBoolean(false);
    }

    protected static CallFunctionReportNode getCallFunctionReportNode(PlanRunnerResult result) {
        return (CallFunctionReportNode) result.getReportTreeAccessor().getChildren(result.getRootReportNode().getId().toString()).next();
    }

    protected static ReportNode getFirstNode(PlanRunnerResult result) {
        return result.getReportTreeAccessor().getChildren(result.getRootReportNode().getId().toString()).next();
    }

    protected static AbstractExecutionEnginePlugin newMyFunctionTypePlugin() {
        return new AbstractExecutionEnginePlugin() {
            @Override
            public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
                super.initializeExecutionContext(executionEngineContext, executionContext);
                FunctionTypeRegistry functionTypeRegistry = executionContext.require(FunctionTypeRegistry.class);
                functionTypeRegistry.registerFunctionType(new MyFunctionType());
            }
        };
    }

    protected static void getLocalAndRemoteTokenFromSession(ExecutionContext t) {
        FunctionGroupHandler.FunctionGroupContext functionGroupContext = (FunctionGroupHandler.FunctionGroupContext) t.getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY);
        FunctionGroupSession session = functionGroupContext.getSession();
        AbstractFunctionHandlerTest.getLocalAndRemoteToken(session);
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

    protected void assertThatLocalAndRemoteTokenHaveBeenReleased() {
        assertTrue(localTokenReturned.get());
        assertTrue(tokenReturned.get());
    }

    protected void markTokenAsReleased(String id) {
        if (localToken.getID().equals(id)) {
            localTokenReturned.set(true);
        }
        if (token.getID().equals(id)) {
            tokenReturned.set(true);
        }
    }

    protected ExecutionEngine newEngineWithCustomTokenReleaseFunction(Consumer<String> tokenReleaseFunction) {
        return ExecutionEngine.builder().withOperationMode(OperationMode.CONTROLLER).withPlugin(new BaseArtefactPlugin()).withPlugin(new FunctionPlugin()).withPlugin(newMyFunctionTypePlugin()).withPlugin(new TokenAutoscalingExecutionPlugin()).withPlugin(new AbstractExecutionEnginePlugin() {
            @Override
            public void initializeExecutionContext(ExecutionEngineContext executionEngineContext,
                                                   ExecutionContext executionContext) {
                executionContext.put(FunctionExecutionService.class, newFunctionExecutionServiceWithCustomTokenReleaseFunction(tokenReleaseFunction));
            }
        }).build();
    }

    protected FunctionExecutionService newFunctionExecutionServiceWithCustomTokenReleaseFunction(Consumer<String> tokenReleaseFunction) {
        return new FunctionExecutionService() {

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
        };
    }

    protected TokenWrapper token(String id) {
        Token localToken_ = new Token();
        localToken_.setId(id);
        localToken_.setAgentid(id);
        return new TokenWrapper(localToken_, new AgentRef());
    }
}
