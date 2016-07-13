package step.grid.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import step.grid.Grid;
import step.grid.client.GridClient;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class AgentTest {
	
	Agent agent;
	
	Grid grid;
	
	GridClient client;

	@Before
	public void init() throws Exception {
		grid = new Grid(8081);
		grid.start();
		
		agent = new Agent("http://localhost:8081", null, 8080);
		Map<String, String> attributes = new HashMap<>();
		attributes.put("att1", "val1");
		agent.addTokens(1, attributes, null);
		agent.start();

		client = new GridClient(grid);
	}
	
	@After
	public void tearDown() throws Exception {
		agent.stop();
		grid.stop();
		client.close();
	}
	
	@Test
	public void test() throws Exception {
		Map<String, Interest> interests = new HashMap<>();
		interests.put("att1", new Interest(Pattern.compile("val.*"), true));
		
		JsonObjectBuilder b = Json.createObjectBuilder();
		b.add("arg1", "val1");
		JsonObject o = b.build();
		
		OutputMessage outputMessage = client.processInput("testFunction", o, "class:step.grid.agent.TestTokenHandler", null, interests);
		Assert.assertEquals(outputMessage.getPayload(), o);;
	}
	
	@Test
	public void testDefault() throws Exception {
		JsonObjectBuilder b = Json.createObjectBuilder();
		b.add("arg1", "val1");
		JsonObject o = b.build();
		
		OutputMessage outputMessage = client.processInput("testFunction", o, "class:step.grid.agent.TestTokenHandler");
		Assert.assertEquals(outputMessage.getPayload(), o);;
	}

}
