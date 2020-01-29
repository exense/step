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
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ContextBuilder;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.reports.ErrorType;
import step.functions.handler.AbstractFunctionHandler;
import step.functions.handler.FunctionHandlerFactory;
import step.functions.io.Input;
import step.functions.io.Output;
import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.agent.tokenpool.TokenSession;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.planbuilder.BaseArtefacts;

public class ArtefactFunctionHandlerTest {

	@Test
	public void test() {
		ExecutionContext context = ContextBuilder.createLocalExecutionContext();

		Return r = new Return();
		r.setOutput(new DynamicValue<String>("{\"Result\":{\"dynamic\":true,\"expression\":\"input.Input1\"}}"));
		
		Plan compositePlan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(r).endBlock().build();
		context.getPlanAccessor().save(compositePlan);
		
		ReportNode parentNode = new ReportNode();
		context.getReportNodeAccessor().save(parentNode);
		
		ArtefactFunctionHandler handler = createArtefactFunctionHandler(context);
		
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

	protected ArtefactFunctionHandler createArtefactFunctionHandler(ExecutionContext context) {
		ArtefactFunctionHandler handler = new ArtefactFunctionHandler();
		
		FunctionHandlerFactory functionHandlerFactory = new FunctionHandlerFactory(new ApplicationContextBuilder(), null);
		
		TokenReservationSession tokenReservationSession = new TokenReservationSession();
		tokenReservationSession.put(AbstractFunctionHandler.EXECUTION_CONTEXT_KEY, context);
		
		functionHandlerFactory.initialize(handler, new TokenSession(), tokenReservationSession, null);
		return handler;
	}
	
	@Test
	public void testError() {
		ExecutionContext context = ContextBuilder.createLocalExecutionContext();

		Script script = new Script();
		script.setScript("output.setError('MyError'");
		
		Plan compositePlan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(script).endBlock().build();
		context.getPlanAccessor().save(compositePlan);
		
		ReportNode parentNode = new ReportNode();
		context.getReportNodeAccessor().save(parentNode);
		
		ArtefactFunctionHandler handler = createArtefactFunctionHandler(context);
		
		Input<JsonObject> input = new Input<>();
		Map<String, String> properties = getInputProperties(compositePlan, parentNode);
		input.setProperties(properties);
		Output<JsonObject> output = handler.handle(input);
		
		Assert.assertEquals("Error in composite keyword", output.getError().getMsg());
		Assert.assertEquals(ErrorType.TECHNICAL, output.getError().getType());
	}

	private Map<String, String> getInputProperties(Plan compositePlan, ReportNode parentNode) {
		Map<String, String> properties = new HashMap<>();
		properties.put(ArtefactFunctionHandler.PLANID_KEY, compositePlan.getId().toString());
		properties.put(AbstractFunctionHandler.PARENTREPORTID_KEY, parentNode.getId().toString());
		return properties;
	}
}
