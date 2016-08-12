package step.grid.agent;

import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import step.grid.agent.handler.Function;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class AnnotatedMethodHandlerTest extends AbstractGridTest {
	
	@Test
	public void test() throws Exception {		
		final String handler = "class:step.grid.agent.handler.AnnotatedMethodHandler";
		JsonObject o = newDummyJson();
		OutputMessage outputMessage = client.getToken().processAndRelease("testFunction", o, handler, null);
		Assert.assertEquals(o,outputMessage.getPayload());
	}
	
	@Function(name="testFunction")
	public static OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		OutputMessage output = new OutputMessage();
		output.setPayload(message.getArgument());
		return output;
	}
}
