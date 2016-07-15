package step.grid.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class AgentTest extends AbstractGridTest {
	
	@Test
	public void test() throws Exception {
		Map<String, Interest> interests = new HashMap<>();
		interests.put("att1", new Interest(Pattern.compile("val.*"), true));
		
		JsonObject o = newDummyJson();
		
		OutputMessage outputMessage = client.getToken(null, interests).processAndRelease("testFunction", o, "class:step.grid.agent.TestTokenHandler", null);
		Assert.assertEquals(outputMessage.getPayload(), o);;
	}
	
	@Test
	public void testDefaultHandler() throws Exception {
		Map<String, Interest> interests = new HashMap<>();
		interests.put("att1", new Interest(Pattern.compile("val2"), true));
		
		Map<String, String> attributes = new HashMap<>();
		attributes.put("att1", "val2");
		
		Map<String, String> properties = new HashMap<>();
		properties.put("tokenhandler.default", "class:step.grid.agent.TestTokenHandler");
		
		addToken(1, attributes, properties);
		
		JsonObject o = newDummyJson();
		
		OutputMessage outputMessage = client.getToken(null, interests).processAndRelease("testFunction", o, null);
		Assert.assertEquals(outputMessage.getPayload(), o);;
	}
	
	@Test
	public void testHandlerChain() throws Exception {		
		final String uri = "class:step.grid.agent.TestTokenHandlerDelegator|class:step.grid.agent.TestTokenHandler";
		JsonObject o = newDummyJson();
		OutputMessage outputMessage = client.getToken().processAndRelease("function1",  o, uri, null);
		Assert.assertEquals(o, outputMessage.getPayload());
		Assert.assertEquals("Test error", outputMessage.getError());
	}
}
