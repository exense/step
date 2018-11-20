package step.plugins.functions.types.composite;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.Return;
import step.artefacts.Script;
import step.artefacts.handlers.CallFunctionHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ContextBuilder;
import step.core.execution.ExecutionContext;
import step.core.plans.LocalPlanRepository;
import step.core.plans.Plan;
import step.core.plans.PlanRepository;
import step.core.plans.builder.PlanBuilder;
import step.core.reports.ErrorType;
import step.functions.io.Input;
import step.functions.io.Output;
import step.grid.Token;
import step.grid.agent.AgentTokenServices;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.planbuilder.BaseArtefacts;

public class ArtefactFunctionHandlerTest {

	@Test
	public void test() {
		ExecutionContext context = ContextBuilder.createLocalExecutionContext();

		Return r = new Return();
		r.setOutput(new DynamicValue<String>("{\"Result\":{\"dynamic\":true,\"expression\":\"input.Input1\"}}"));
		
		Plan compositePlan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(r).endBlock().build();
		PlanRepository planRepository = new LocalPlanRepository(context.getArtefactAccessor());
		planRepository.save(compositePlan);
		
		ReportNode parentNode = new ReportNode();
		context.getReportNodeAccessor().save(parentNode);
		
		ArtefactFunctionHandler handler = new ArtefactFunctionHandler();
		
		AgentTokenWrapper agentToken = getMockedAgentToken(context);
		
		handler.initialize(agentToken);
		
		Input<JsonObject> input = new Input<>();
		input.setPayload(Json.createObjectBuilder().add("Input1", "InputValue1").build());
		Map<String, String> properties = getInputProperties(compositePlan, parentNode);
		input.setProperties(properties);
		Output<JsonObject> output = handler.handle(input);
		
		Assert.assertNull(output.getError());
		Assert.assertEquals("InputValue1", output.getPayload().getString("Result"));
		
		AtomicInteger count = new AtomicInteger(0);
		context.getReportNodeAccessor().getAll().forEachRemaining(n->count.incrementAndGet());
		Assert.assertEquals(4, count.get());
	}
	
	@Test
	public void testError() {
		ExecutionContext context = ContextBuilder.createLocalExecutionContext();

		Script script = new Script();
		script.setScript("output.setError('MyError'");
		
		Plan compositePlan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(script).endBlock().build();
		PlanRepository planRepository = new LocalPlanRepository(context.getArtefactAccessor());
		planRepository.save(compositePlan);
		
		ReportNode parentNode = new ReportNode();
		context.getReportNodeAccessor().save(parentNode);
		
		ArtefactFunctionHandler handler = new ArtefactFunctionHandler();
		
		AgentTokenWrapper agentToken = getMockedAgentToken(context);
		
		handler.initialize(agentToken);
		
		Input<JsonObject> input = new Input<>();
		Map<String, String> properties = getInputProperties(compositePlan, parentNode);
		input.setProperties(properties);
		Output<JsonObject> output = handler.handle(input);
		
		Assert.assertEquals("Error in composite keyword", output.getError().getMsg());
		Assert.assertEquals(ErrorType.TECHNICAL, output.getError().getType());
	}

	private Map<String, String> getInputProperties(Plan compositePlan, ReportNode parentNode) {
		Map<String, String> properties = new HashMap<>();
		properties.put(CallFunctionHandler.ARTEFACTID, compositePlan.getRoot().getId().toString());
		properties.put(CallFunctionHandler.PARENTREPORTID, parentNode.getId().toString());
		return properties;
	}

	private AgentTokenWrapper getMockedAgentToken(ExecutionContext context) {
		Token token = new Token();
		token.attachObject(CallFunctionHandler.EXECUTION_CONTEXT_KEY, context);
		
		AgentTokenWrapper agentToken = new AgentTokenWrapper(token);
		AgentTokenServices tokenServices = new AgentTokenServices(null);
		tokenServices.setApplicationContextBuilder(new ApplicationContextBuilder());
		agentToken.setServices(tokenServices);
		return agentToken;
	}

}
