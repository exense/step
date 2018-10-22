package step.grid.client;

import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import step.grid.TokenWrapper;
import step.grid.agent.AbstractGridTest;
import step.grid.agent.TestTokenHandler;
import step.grid.client.GridClientImpl.AgentCallTimeoutException;

public class GridClientTest extends AbstractGridTest {
	
	@Before
	public void init() throws Exception {
		super.init();
	}
	
	@Test
	public void testAgentCallTimeoutDuringReservation() throws Exception {
		getClient(0,1,1);
		
		Exception actualException = null;
		try {
			selectToken();
		} catch (Exception e) {
			actualException = e;
		}
		
		Assert.assertNotNull(actualException);
		Assert.assertTrue(actualException instanceof AgentCallTimeoutException);
	}
	
	@Test
	public void testAgentCallTimeoutDuringRelease() throws Exception {
		getClient(0,10000,1);
		
		TokenWrapper token = selectToken();

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
