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
package step.core.variables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.execution.ReportNodeCache;

public class VariablesManager {
	
	private final ExecutionContext context;
	
	private final ReportNodeCache nodeCache;
		
	private ConcurrentHashMap<String, Map<String, Variable>> register = new ConcurrentHashMap<>();
		
	public VariablesManager(ExecutionContext context) {
		super();
		this.context = context;
		this.nodeCache = context.getReportNodeCache();
	}
	
	public void removeVariable(ReportNode node, String key) {
		Map<String, Variable> variableMap = getVariableMap(node.getId().toString(), false);
		if(variableMap!=null) {
			variableMap.remove(key);
		}
	}
	
	public void updateVariable(String key, Object value) throws ImmutableVariableException {
		ReportNode currentNode = context.getCurrentReportNode();
		Variable closestVariable = getVariable_(currentNode, key, true);
		if(closestVariable!=null) {
			if(closestVariable.getType()==VariableType.NORMAL) {
				closestVariable.setValue(value);	
			} else {
				throw new ImmutableVariableException(key);
			}
		} else {
			throw new UndefinedVariableException(key);
		}
	}
	
	public void putVariable(ReportNode targetNode, String key, Object value) {
		putVariable(targetNode, VariableType.NORMAL, key, value);
	}
	
	public void putVariable(ReportNode targetNode,VariableType type, String key, Object value) {
		Map<String, Variable> variableMap;
		variableMap = getVariableMap(targetNode.getId().toString(), true);		
		Variable variable = new Variable(value, type);
		variableMap.put(key, variable);
	}
	
	public Object getVariable(String key) {
		ReportNode currentNode = context.getCurrentReportNode();
		return getVariable(currentNode, key, true);
	}
	
	private void assertNotNull(Object o, String key) {
		if(o == null) {
			throw new UndefinedVariableException(key);
		}
	}
	
	/**
	 * @param key key
	 * @throws UndefinedVariableException if the variable is undefined
	 */
	public String getVariableAsString(String key) {
		Object o = getVariable(key);
		assertNotNull(o, key);
		return o.toString();
	}
	
	public String getVariableAsString(String key, String default_) {
		try {
			return getVariableAsString(key);			
		} catch(UndefinedVariableException e) {
			return default_;
		}
	}
	
	/**
	 * @throws UndefinedVariableException if the variable is undefined
	 */
	public Integer getVariableAsInteger(String key) {
		Object o = getVariable(key);
		assertNotNull(o, key);
		return Integer.parseInt(o.toString());
	}
	
	public Integer getVariableAsInteger(String key, Integer default_) {
		try {
			return getVariableAsInteger(key);			
		} catch(UndefinedVariableException e) {
			return default_;
		}
	}
	
	public Boolean getVariableAsBoolean(String key, boolean default_) {
		try {
			return getVariableAsBoolean(key);			
		} catch(UndefinedVariableException e) {
			return default_;
		}
	}
	
	/**
	 * @throws UndefinedVariableException if the variable is undefined
	 */
	public Boolean getVariableAsBoolean(String key) {
		Object o = getVariable(key);
		assertNotNull(o, key);
		return Boolean.parseBoolean(o.toString());
	}
	 
	public Object getVariable(ReportNode node, String key, boolean recursive) {
		Variable variable = getVariable_(node, key, recursive);
		return variable!=null?variable.getValue():null;
	}
	
	private Variable getVariable_(ReportNode node, String key, boolean recursive) {
		Variable variable = null;
		ReportNode currentNode = node;
		do {
			Map<String, Variable> variableMap = getVariableMap(currentNode.getId().toString(), false);
			if(variableMap!=null) {
				variable = variableMap.get(key);
			}
		} while (recursive && 
				variable==null &&
				currentNode.getParentID()!=null && 
				(currentNode = nodeCache.get(currentNode.getParentID().toString()))!=null);
		return variable;
	}

	
	private Map<String, Variable> getVariableMap(String nodeId, boolean createIfNotExisting) { 
		Map<String, Variable>variableMap = register.get(nodeId);
		if(createIfNotExisting && variableMap==null) {
			variableMap = new ConcurrentHashMap<String, Variable>();
			Map<String, Variable> previous = register.putIfAbsent(nodeId, variableMap);
			if(previous!=null) {
				variableMap = previous;
			}
		}
		return variableMap;
	}
	
	public Map<String, Object> getAllVariables() {	
		Map<String, Object> result = new HashMap<>();
		ReportNode currentNode = context.getCurrentReportNode();
		do {
			Map<String, Variable> variableMap = register.get(currentNode.getId().toString());
			if(variableMap!=null) {
				for(String variableName:variableMap.keySet()) {
					if(!result.containsKey(variableName)) {
						Object value = variableMap.get(variableName).getValue();
						result.put(variableName, value);
					}
				}
			}
		} while (currentNode.getParentID()!=null && 
				(currentNode = nodeCache.get(currentNode.getParentID().toString()))!=null);
		return result;
	}
	
	public List<Object> getAllVariables(String key) {
		List<Object> result = new ArrayList<>();
		ReportNode currentNode = context.getCurrentReportNode();
		do {
			Object variable = getVariable(currentNode, key, false);
			if(variable != null) {
				result.add(variable);
			}
		} while (currentNode.getParentID()!=null && 
				(currentNode = nodeCache.get(currentNode.getParentID().toString()))!=null);
		return result;
	}
	
	public Object getFirstVariableMatching(Pattern pattern) {		
		ReportNode currentNode = context.getCurrentReportNode();
		return getFirstVariableMatching(currentNode, pattern);
	}

	public Object getFirstVariableMatching(ReportNode node, Pattern pattern) {	
		Variable result = null;
		ReportNode currentNode = node;
		do {
			Map<String, Variable> variables = register.get(currentNode.getId().toString());
			if(variables!=null) {
				Matcher matcher = pattern.matcher("");
				for(String variableName:variables.keySet()) {
					matcher.reset(variableName);
					if(matcher.matches()) {
						result = variables.get(variableName);
					}
				}
			}
		} while (result==null &&
				currentNode.getParentID()!=null && 
				(currentNode = nodeCache.get(currentNode.getParentID().toString()))!=null);
		return result!=null?result.getValue():null;
	}
	
	public void releaseVariables(String nodeId) {
		register.remove(nodeId);
	}
}
