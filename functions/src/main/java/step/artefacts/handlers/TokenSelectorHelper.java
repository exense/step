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
package step.artefacts.handlers;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.json.JsonObject;
import javax.json.spi.JsonProvider;

import step.artefacts.TokenSelector;
import step.common.managedoperations.OperationManager;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.functions.FunctionExecutionService;
import step.grid.TokenWrapper;
import step.grid.tokenpool.Interest;

public class TokenSelectorHelper {
	
	protected final FunctionExecutionService functionExecutionService;

	private static JsonProvider jprov = JsonProvider.provider();
	
	protected DynamicJsonObjectResolver dynamicJsonObjectResolver;
	
	public TokenSelectorHelper(FunctionExecutionService functionClient, DynamicJsonObjectResolver dynamicJsonObjectResolver) {
		super();
		this.functionExecutionService = functionClient;
		this.dynamicJsonObjectResolver = dynamicJsonObjectResolver;
	}
	
	protected TokenWrapper selectToken(TokenSelector testArtefact, FunctionExecutionService functionExecutionService, Map<String, Object> bindings) {
		TokenWrapper tokenHandle;
		String token = testArtefact.getToken().get();
		if(token!=null) {
			JsonObject selectionCriteriaBeforeEvaluation = jprov.createReader(new StringReader(token)).readObject();
			
			JsonObject selectionCriteriaJson = dynamicJsonObjectResolver.evaluate(selectionCriteriaBeforeEvaluation, bindings);
			
			if(!testArtefact.getRemote().get()) {
				tokenHandle = selectLocalToken();
			} else {
				Map<String, Interest> selectionCriteria = new HashMap<>();
				selectionCriteriaJson.keySet().stream().forEach(key->selectionCriteria.put(key, new Interest(Pattern.compile(selectionCriteriaJson.getString(key)), true)));
				
				OperationManager.getInstance().enter("Token selection", selectionCriteria);
				try {
					tokenHandle = functionExecutionService.getTokenHandle(null, selectionCriteria);
				} finally {
					OperationManager.getInstance().exit();					
				}
			}
		} else {
			throw new RuntimeException("Token field hasn't been specified");
		}
		return tokenHandle;
	}
	
	protected TokenWrapper selectLocalToken() {
		TokenWrapper tokenHandle = functionExecutionService.getLocalTokenHandle();
		return tokenHandle;
	}
	
	protected void returnToken(TokenWrapper token) {
		functionExecutionService.returnTokenHandle(token);
	}
}
