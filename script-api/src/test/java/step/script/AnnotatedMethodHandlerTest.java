package step.script;

import org.junit.Test;

import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class AnnotatedMethodHandlerTest {
	
	@Test
	public void test() throws Exception {		
		// TODO implement
	}
	
	@Function(name="testFunction")
	public static OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		OutputMessage output = new OutputMessage();
		output.setPayload(message.getArgument());
		return output;
	}
}
