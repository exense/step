package step.grid;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import step.grid.agent.AbstractGridTest;
import step.grid.client.GridClientConfiguration;
import step.grid.client.GridClientImpl;
import step.grid.io.AgentErrorCode;
import step.grid.io.OutputMessage;

public class GridTest extends AbstractGridTest {
	
	@Before
	public void init() throws Exception {
		super.init();
		
		GridClientConfiguration gridClientConfiguration = new GridClientConfiguration();
		gridClientConfiguration.setNoMatchExistsTimeout(2000);
		client = new GridClientImpl(gridClientConfiguration, grid, grid);
	}
	
	@Test
	public void testHappyPath() throws Exception {
		TokenWrapper token = selectToken();
		callToken(token);
		
		Assert.assertEquals(token.getState(), TokenWrapperState.IN_USE);
		token.setCurrentOwner(new TokenWrapperOwner() {});
		
		returnToken(token);
		
		Assert.assertEquals(token.getState(), TokenWrapperState.FREE);
		Assert.assertNull(token.getCurrentOwner());	
	
		TokenWrapper token2 = selectToken();
		Assert.assertEquals(token2, token);
		returnToken(token2);
		
		Assert.assertEquals(token2.getState(), TokenWrapperState.FREE);
	}
	
	@Test
	public void testTokenError() throws Exception {
		TokenWrapper token = selectToken();
		OutputMessage outputMessage = callTokenAndProduceAgentError(token);
		
		Assert.assertEquals(AgentErrorCode.TIMEOUT_REQUEST_NOT_INTERRUPTED, outputMessage.getAgentError().getErrorCode());
		Assert.assertEquals(token.getState(), TokenWrapperState.ERROR);

		returnToken(token);
		removeTokenError(token);
		
		Assert.assertEquals(token.getState(), TokenWrapperState.FREE);
		
		token = selectToken();
		outputMessage = callTokenAndProduceAgentError(token);
		returnToken(token);
		
		Assert.assertEquals(token.getState(), TokenWrapperState.ERROR);
		
		Exception actualException = null;
		try {
			token = selectToken();			
		} catch (Exception e) {
			actualException = e;
		}
		
		Assert.assertNotNull(actualException);
		Assert.assertTrue(actualException.getMessage().contains("Not able to find any agent token"));
		
		removeTokenError(token);
		
		token = selectToken();	
		outputMessage = callToken(token);
		
		Assert.assertEquals(token.getState(), TokenWrapperState.IN_USE);
		
		returnToken(token);
		
		Assert.assertEquals(token.getState(), TokenWrapperState.FREE);
	}
	
	@Test
	public void testMaintenance() throws Exception {
		TokenWrapper token = selectToken();
		OutputMessage outputMessage = callToken(token);
		
		Assert.assertNull(outputMessage.getAgentError());
		Assert.assertEquals(token.getState(), TokenWrapperState.IN_USE);

		startTokenMaintenance(token);
		
		Assert.assertEquals(token.getState(), TokenWrapperState.MAINTENANCE_REQUESTED);
		
		returnToken(token);
		
		Assert.assertEquals(token.getState(), TokenWrapperState.MAINTENANCE);
		
		stopTokenMaintenance(token);
		Assert.assertEquals(token.getState(), TokenWrapperState.FREE);
	}
}
