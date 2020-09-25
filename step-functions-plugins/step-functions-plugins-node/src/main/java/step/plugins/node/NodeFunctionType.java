/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.plugins.node;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import step.functions.type.AbstractFunctionType;
import step.functions.type.SetupFunctionException;
import step.grid.agent.AgentTypes;
import step.grid.tokenpool.Interest;

public class NodeFunctionType extends AbstractFunctionType<NodeFunction> {

	public static final String FILE = "$node.js.file";
	
	@Override
	public void init() {
		super.init();
	}

	@Override
	public String getHandlerChain(NodeFunction function) {
		return "";
	}

	@Override
	public Map<String, String> getHandlerProperties(NodeFunction function) {
		Map<String, String> props = new HashMap<>();
		registerFile(function.getJsFile(), FILE, props);
		return props;
	}

	@Override
	public Map<String, Interest> getTokenSelectionCriteria(NodeFunction function) {
		Map<String, Interest> criteria = new HashMap<>();
		criteria.put(AgentTypes.AGENT_TYPE_KEY, new Interest(Pattern.compile("node"), true));
		return criteria;
	}

	@Override
	public NodeFunction newFunction() {
		return new NodeFunction();
	}

	@Override
	public void setupFunction(NodeFunction function) throws SetupFunctionException {
		super.setupFunction(function);
	}

}
