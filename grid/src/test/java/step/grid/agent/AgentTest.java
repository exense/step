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
		
		JsonObject o = Json.createObjectBuilder().add("a", "b").build();
		
		OutputMessage outputMessage = client.getToken(null, interests).processAndRelease("testFunction", o, "class:step.grid.agent.TestTokenHandler", null);
		Assert.assertEquals(outputMessage.getPayload(), o);;
	}
}
