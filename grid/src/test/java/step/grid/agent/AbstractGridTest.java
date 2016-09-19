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

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;

import step.grid.Grid;
import step.grid.agent.conf.AgentConf;
import step.grid.client.GridClient;

public abstract class AbstractGridTest {

	protected Agent agent;
	
	protected Grid grid;
	
	protected GridClient client;
	
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
		grid = new Grid(8081);
		grid.start();
		
		agent = new Agent(new AgentConf("http://localhost:8081", 8080, null));
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("att1", "val1");
		agent.addTokens(nTokens, attributes, null, null);
		agent.setRegistrationIntervalMs(100);
		agent.start();

		client = new GridClient(grid);
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
}
