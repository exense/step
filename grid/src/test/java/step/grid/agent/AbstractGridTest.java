/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.grid.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;

import step.grid.Grid;
import step.grid.TokenWrapper;
import step.grid.agent.conf.AgentConf;
import step.grid.client.GridClientImpl;
import step.grid.client.GridClientImpl.AgentCommunicationException;
import step.grid.io.AgentErrorCode;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public abstract class AbstractGridTest {

	protected Agent agent;
	
	protected Grid grid;
	
	protected GridClientImpl client;
	
	int nTokens = 1;

	public AbstractGridTest() {
		super();
	}

	public AbstractGridTest(int nTokens) {
		super();
		this.nTokens = nTokens;
	}

	@Before
	public void init() throws Exception {
		grid = new Grid(0);
		grid.start();
				
		agent = new Agent(new AgentConf("http://localhost:"+grid.getServerPort(), 0, null, 100));
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("att1", "val1");
		agent.addTokens(nTokens, attributes, null, null);
		agent.start();

		client = new GridClientImpl(grid, grid);
	}
	
	protected void addToken(int count, Map<String, String> attributes) {
		agent.addTokens(nTokens, attributes, null, null);
	}
	
	protected void addToken(int count, Map<String, String> attributes, Map<String, String> properties) {
		agent.addTokens(nTokens, attributes, null, properties);
		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@After
	public void tearDown() throws Exception {
		agent.stop();
		grid.stop();
		client.close();
	}
	
	protected JsonObject newDummyJson() {
		return Json.createObjectBuilder().add("a", "b").build();
	}
	
	protected void stopTokenMaintenance(TokenWrapper token) {
		grid.stopTokenMaintenance(token.getID());
	}

	protected void startTokenMaintenance(TokenWrapper token) {
		grid.startTokenMaintenance(token.getID());
	}

	protected void removeTokenError(TokenWrapper token) {
		grid.removeTokenError(token.getID());
	}

	protected void returnToken(TokenWrapper token) throws AgentCommunicationException {
		client.returnTokenHandle(token);
	}

	protected OutputMessage callTokenAndProduceAgentError(TokenWrapper token) throws Exception {
		JsonObject o = Json.createObjectBuilder().add("agentError", AgentErrorCode.TIMEOUT_REQUEST_NOT_INTERRUPTED.toString()).build();
		return client.call(token, "testFunction", o, TestTokenHandler.class.getName(), null, null, 1000);
	}
	
	protected OutputMessage callToken(TokenWrapper token) throws Exception {
		JsonObject o = newDummyJson();
		return client.call(token, "testFunction", o, TestTokenHandler.class.getName(), null, null, 1000);
	}

	protected TokenWrapper selectToken() throws AgentCommunicationException {
		Map<String, Interest> interests = new HashMap<>();
		interests.put("att1", new Interest(Pattern.compile("val.*"), true));
		TokenWrapper token = client.getTokenHandle(null, interests, true);
		return token;
	}
}
