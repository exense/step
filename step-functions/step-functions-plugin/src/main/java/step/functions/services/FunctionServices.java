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
package step.functions.services;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.functions.Function;
import step.functions.editors.FunctionEditor;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.manager.FunctionManager;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;
import step.grid.TokenWrapper;
import step.grid.tokenpool.Interest;
import step.resources.ResourceManager;

@Path("/functions")
public class FunctionServices extends AbstractServices {

	protected ReportNodeAttachmentManager reportNodeAttachmentManager;
	
	protected FunctionManager functionManager;
	
	protected FunctionExecutionService functionExecutionService;
	
	@PostConstruct
	public void init() {
		reportNodeAttachmentManager = new ReportNodeAttachmentManager(getContext().get(ResourceManager.class));
		functionManager = getContext().get(FunctionManager.class);
		functionExecutionService = getContext().get(FunctionExecutionService.class);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/")
	@Secured(right="kw-write")
	public Function save(Function function) throws SetupFunctionException, FunctionTypeException {
		return functionManager.saveFunction(function);
	}
	
	@POST
	@Path("/{id}/copy")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="kw-write")
	public void copyFunction(@PathParam("id") String id) throws FunctionTypeException {		
		functionManager.copyFunction(id);
	}
	
	@DELETE
	@Path("/{id}")
	@Secured(right="kw-delete")
	public void delete(@PathParam("id") String functionId) throws FunctionTypeException {
		functionManager.deleteFunction(functionId);
	}
	
	@POST
	@Path("/search")
	@Secured(right="kw-read")
	public Function get(Map<String,String> attributes) {
		return functionManager.getFunctionByAttributes(attributes);
	}
	
	@GET
	@Path("/{id}")
	@Secured(right="kw-read")
	public Function get(@PathParam("id") String functionId) {
		return functionManager.getFunctionById(functionId);
	}
	
	@GET
	@Path("/{id}/editor")
	@Secured(right="kw-read")
	public String getFunctionEditor(@PathParam("id") String functionId) {
		Function function = functionManager.getFunctionById(functionId);
		FunctionEditor editor = getContext().get(FunctionEditorRegistry.class).getFunctionEditor(function);
		if(editor!=null) {
			return editor.getEditorPath(function);
		} else {
			return null;
		}
	}
	
	@GET
	@Path("/types/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="kw-read")
	public Function newFunctionTypeConf(@PathParam("id") String type) {
		Function newFunction = functionManager.newFunction(type);
		newFunction.setAttributes(new HashMap<>());
		newFunction.getAttributes().put(Function.NAME, "");
		newFunction.setSchema(Json.createObjectBuilder().build());
		return newFunction;
	}

	public static class GetTokenHandleParameter {
		
		Map<String, String> attributes;
		Map<String, Interest> interests;
		boolean createSession;

		public Map<String, String> getAttributes() {
			return attributes;
		}
		
		public void setAttributes(Map<String, String> attributes) {
			this.attributes = attributes;
		}
		
		public Map<String, Interest> getInterests() {
			return interests;
		}
		
		public void setInterests(Map<String, Interest> interests) {
			this.interests = interests;
		}
		
		public boolean isCreateSession() {
			return createSession;
		}
		
		public void setCreateSession(boolean createSession) {
			this.createSession = createSession;
		}
	}
	
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/executor/tokens/select")
	@Secured(right="kw-execute")
	public TokenWrapper getTokenHandle(GetTokenHandleParameter parameter) throws FunctionExecutionServiceException {
		return functionExecutionService.getTokenHandle(parameter.attributes, parameter.interests, parameter.createSession);
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/executor/tokens/return")
	@Secured(right="kw-execute")
	public void returnTokenHandle(TokenWrapper token) throws FunctionExecutionServiceException {
		functionExecutionService.returnTokenHandle(token);
	}
	
	public static class CallFunctionInput {
		
		String functionId;
		Map<String, String> functionAttributes;
		TokenWrapper tokenHandle;
		Input<JsonObject> input;
		
		public CallFunctionInput() {
			super();
		}

		public String getFunctionId() {
			return functionId;
		}

		public void setFunctionId(String functionId) {
			this.functionId = functionId;
		}

		public TokenWrapper getTokenHandle() {
			return tokenHandle;
		}

		public void setTokenHandle(TokenWrapper tokenHandle) {
			this.tokenHandle = tokenHandle;
		}

		public Map<String, String> getFunctionAttributes() {
			return functionAttributes;
		}

		public void setFunctionAttributes(Map<String, String> functionAttributes) {
			this.functionAttributes = functionAttributes;
		}

		public Input<JsonObject> getInput() {
			return input;
		}

		public void setInput(Input<JsonObject> input) {
			this.input = input;
		}
		
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/executor/execute")
	@Secured(right="kw-execute")
	public Output<JsonObject> callFunction(CallFunctionInput input) {
		Function function;
		if(input.functionId!=null) {
			function = functionManager.getFunctionById(input.getFunctionId());
		} else {
			function = functionManager.getFunctionByAttributes(input.getFunctionAttributes());
		}
		return functionExecutionService.callFunction(input.tokenHandle, function, input.input, JsonObject.class);			
	}
}
