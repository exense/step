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
package step.handlers.processhandler;

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;

import org.junit.Test;

import junit.framework.Assert;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class ProcessHandlerTest {

	@Test 
	public void testt1() throws Exception {
		Map<String, String> properties = new HashMap<>();
		ProcessHandler processhandler = new ProcessHandler();
		
		String echoCmd;
		if(System.getProperty("os.name").startsWith("Windows")) {
			echoCmd = "cmd.exe /r echo test";;
		} else {
			echoCmd = "echo test";
		}
		
		InputMessage message = new InputMessage();
		message.setProperties(properties);
		message.setArgument(Json.createObjectBuilder().add("cmd", echoCmd).build());
		message.setCallTimeout(10000);
		
		OutputMessage out = processhandler.handle(new AgentTokenWrapper(), message);
		
		Assert.assertEquals("test\n",out.getPayload().getString("stdout"));
	}

}
