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
package step.plugins.adaptergrid;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

import step.artefacts.handlers.CallFunctionHandler;
import step.attachments.AttachmentMeta;
import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.functions.Function;
import step.functions.FunctionClient;
import step.functions.FunctionExecutionService;
import step.functions.FunctionRepository;
import step.functions.Input;
import step.functions.Output;
import step.functions.editors.FunctionEditor;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.routing.FunctionRouter;
import step.functions.type.SetupFunctionException;
import step.grid.TokenWrapper;
import step.grid.client.GridClient.AgentCommunicationException;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.grid.tokenpool.Interest;

@Path("/functions")
public class FunctionRepositoryServices extends AbstractServices {

	ReportNodeAttachmentManager reportNodeAttachmentManager;
	
	private FunctionClient getFunctionClient() {
		return (FunctionClient) getContext().get(GridPlugin.FUNCTIONCLIENT_KEY);
	}
	
	private FunctionRepository getFunctionRepository() {
		return getFunctionClient().getFunctionRepository();
	}
	
	@PostConstruct
	public void init() {
		reportNodeAttachmentManager = new ReportNodeAttachmentManager(getContext().getAttachmentManager());
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/")
	@Secured(right="kw-write")
	public Function save(Function function) throws SetupFunctionException {
		FunctionRepository repo = getFunctionRepository();
		if(function.getId()==null || repo.getFunctionById(function.getId().toString())==null) {
			getFunctionClient().setupFunction(function);
		}
		repo.addFunction(function);
		return function;
	}
	
	public static class ExecutionOutput {
		
		Output output;
		
		List<AttachmentMeta> attachments;

		public ExecutionOutput() {
			super();
		}

		public Output getOutput() {
			return output;
		}

		public void setOutput(Output output) {
			this.output = output;
		}

		public List<AttachmentMeta> getAttachments() {
			return attachments;
		}

		public void setAttachments(List<AttachmentMeta> attachments) {
			this.attachments = attachments;
		}
	}
	
	public static class ExecutionInput {
		
		String argument;
		
		Map<String, String> properties;

		public ExecutionInput() {
			super();
		}

		public String getArgument() {
			return argument;
		}

		public void setArgument(String argument) {
			this.argument = argument;
		}

		public Map<String, String> getProperties() {
			return properties;
		}

		public void setProperties(Map<String, String> properties) {
			this.properties = properties;
		}
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/execute")
	@Secured(right="kw-execute")
	public ExecutionOutput executeFunction(@PathParam("id") String functionId, ExecutionInput executionInput) {
		ExecutionOutput result = new ExecutionOutput();
		Output output;
		Function function = get(functionId);
		
		FunctionExecutionService client = getFunctionClient();
		try {
			FunctionRouter functionRouter = getContext().get(FunctionRouter.class);
			TokenWrapper token = functionRouter.selectToken(function, null, null);
			try {
				ExecutionContext executionContext = createContext(getContext());
				token.getToken().attachObject(CallFunctionHandler.EXECUTION_CONTEXT_KEY, executionContext);
				ExecutionContext.setCurrentContext(executionContext);
				Input input = new Input();		
				
				JsonObject inputBeforeEvalution;
				String argument = executionInput.getArgument();
				if(argument!=null&&argument.length()>0) {
					inputBeforeEvalution = Json.createReader(new StringReader(argument)).readObject();				
				} else {
					inputBeforeEvalution = Json.createObjectBuilder().build();
				}
				
				DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(getContext().getExpressionHandler()));
				JsonObject inputAfterEvaluation = dynamicJsonObjectResolver.evaluate(inputBeforeEvalution, new HashMap<>());
				
				input.setArgument(inputAfterEvaluation);
				input.setProperties(executionInput.getProperties());
				
				output = client.callFunction(token, functionId, input);				
			} finally {
				client.returnTokenHandle(token);
			}
		} catch(Exception e) {
			output = new Output();
			FunctionClient.attachExceptionToOutput(output, e);
		}
		List<AttachmentMeta> attachmentMetas = new ArrayList<>();
		result.setAttachments(attachmentMetas);
		if(output.getAttachments()!=null) {
			for(Attachment a:output.getAttachments()) {
				AttachmentMeta attachmentMeta;
				attachmentMeta = reportNodeAttachmentManager.createAttachmentWithoutQuotaCheck(AttachmentHelper.hexStringToByteArray(a.getHexContent()), a.getName());
				attachmentMetas.add(attachmentMeta);
			}
		}
		result.setOutput(output);
		return result;
	}
	
	@POST
	@Path("/{id}/copy")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="kw-write")
	public void copyFunction(@PathParam("id") String id) {		
		FunctionRepository repo = getFunctionRepository();
		Function source = repo.getFunctionById(id);
		if(source!=null) {
			Function copy = getFunctionClient().copyFunction(source);
			repo.addFunction(copy);
		}
	}
	
	public static ExecutionContext createContext(GlobalContext g) {
		ReportNode root = new ReportNode();
		ExecutionContext c = new ExecutionContext("");
		c.setGlobalContext(g);
		c.getReportNodeCache().put(root);
		c.setReport(root);
		ExecutionContext.setCurrentReportNode(root);
		c.setExecutionParameters(new ExecutionParameters("dummy", null, ExecutionMode.RUN));
		return c;
	}
	
	@DELETE
	@Path("/{id}")
	@Secured(right="kw-delete")
	public void delete(@PathParam("id") String functionId) {
		getFunctionRepository().deleteFunction(functionId);
	}
	
	@POST
	@Path("/search")
	@Secured(right="kw-read")
	public Function get(Map<String,String> attributes) {
		return getFunctionRepository().getFunctionByAttributes(attributes);
	}
	
	@GET
	@Path("/{id}")
	@Secured(right="kw-read")
	public Function get(@PathParam("id") String functionId) {
		return getFunctionRepository().getFunctionById(functionId);
	}
	
	@GET
	@Path("/{id}/editor")
	@Secured(right="kw-read")
	public String getFunctionEditor(@PathParam("id") String functionId) {
		Function function = getFunctionRepository().getFunctionById(functionId);
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
		Function newFunction = getFunctionClient().newFunction(type);
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
	public TokenWrapper getTokenHandle(GetTokenHandleParameter parameter) throws AgentCommunicationException {
		return getFunctionClient().getTokenHandle(parameter.attributes, parameter.interests, parameter.createSession);
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/executor/tokens/return")
	@Secured(right="kw-execute")
	public void returnTokenHandle(TokenWrapper token) throws AgentCommunicationException {
		getFunctionClient().returnTokenHandle(token);
	}
	
	public static class CallFunctionInput {
		
		String functionId;
		Map<String, String> functionAttributes;
		TokenWrapper tokenHandle;
		Input input;
		
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

		public Input getInput() {
			return input;
		}

		public void setInput(Input input) {
			this.input = input;
		}
		
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/executor/execute")
	@Secured(right="kw-execute")
	public Output callFunction(CallFunctionInput input) {
		if(input.functionId!=null) {
			return getFunctionClient().callFunction(input.tokenHandle, input.functionId, input.input);			
		} else {
			return getFunctionClient().callFunction(input.tokenHandle, input.functionAttributes, input.input);			
		}
	}
}
