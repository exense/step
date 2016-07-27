package step.grid;

import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import step.grid.agent.handler.Function;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class AnnotatedMethodHandlerTest {
	
	@Function(name="function3")
	public static OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		OutputMessage output = new OutputMessage();
		output.setPayload(message.getArgument());
		return output;
	}
}
