package step.grid.client;

import java.io.File;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import step.commons.helpers.FileHelper;
import step.grid.TokenWrapper;
import step.grid.agent.AbstractGridTest;
import step.grid.agent.TestTokenHandler;
import step.grid.client.GridClientImpl.AgentCallTimeoutException;
import step.grid.io.OutputMessage;

public class GridClientTest extends AbstractGridTest {
	
	@Before
	public void init() throws Exception {
		super.init();
	}
	
	@Test
	public void testFileRegistration() throws Exception {
		getClient(0,10000,10000);
		
		TokenWrapper token = selectToken();

		File testFile = new File(this.getClass().getResource("TestFile").getFile());
		String fileHandle = client.registerFile(testFile);
		
		JsonObject input = Json.createObjectBuilder().add("file", fileHandle).add("fileVersion", FileHelper.getLastModificationDateRecursive(testFile)).build();
		
		OutputMessage output = client.call(token, "test", input, TestMessageHandler.class.getName(), null, new HashMap<>(), 10000);
		
		Assert.assertEquals("TEST", output.getPayload().getString("content"));
	}

	// AgentCallTimeout during reservation is currently impossible to test as we don't have any hook in the reservation where to inject a sleep
	
	@Test
	public void testAgentCallTimeoutDuringRelease() throws Exception {
		getClient(0,10000,1);
		
		TokenWrapper token = selectToken();

		
		client.call(token, "test", newDummyJson(), TestMessageHandler.class.getName(), null, new HashMap<>(), 1000);
		
		Exception actualException = null;
		try {
			returnToken(token);
		} catch (Exception e) {
			actualException = e;
		}
		
		Assert.assertNotNull(actualException);
		Assert.assertTrue(actualException instanceof AgentCallTimeoutException);
	}
	
	@Test
	public void testAgentCallTimeoutException() throws Exception {
		getClient(0, 10000, 10000);
		
		TokenWrapper token = selectToken();
		
		Exception actualException = null;
		JsonObject o = newDummyJson();
		try {
			client.call(token, "testFunction", o, TestTokenHandler.class.getName(), null, null, 1);			
		} catch (Exception e) {
			actualException = e;
		}
		
		Assert.assertNotNull(actualException);
		Assert.assertTrue(actualException instanceof AgentCallTimeoutException);
	}

	protected void getClient(int readOffset, int reserveTimeout, int releaseTimeout) {
		GridClientConfiguration gridClientConfiguration = new GridClientConfiguration();
		gridClientConfiguration.setReadTimeoutOffset(readOffset);
		gridClientConfiguration.setReserveSessionTimeout(reserveTimeout);
		gridClientConfiguration.setReleaseSessionTimeout(releaseTimeout);
		client = new GridClientImpl(gridClientConfiguration, grid, grid);
	}
}
