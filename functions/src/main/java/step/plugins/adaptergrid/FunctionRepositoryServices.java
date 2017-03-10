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

import javax.json.Json;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import step.attachments.AttachmentMeta;
import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.core.miscellaneous.ReportNodeAttachmentManager.AttachmentQuotaException;
import step.functions.Function;
import step.functions.FunctionClient;
import step.functions.FunctionClient.FunctionTokenHandle;
import step.functions.FunctionRepository;
import step.functions.Input;
import step.functions.Output;
import step.functions.editors.FunctionEditor;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.type.SetupFunctionException;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;

@Path("/functions")
public class FunctionRepositoryServices extends AbstractServices {

	private FunctionClient getFunctionClient() {
		return (FunctionClient) getContext().get(GridPlugin.FUNCTIONCLIENT_KEY);
	}
	
	private FunctionRepository getFunctionRepository() {
		return getFunctionClient().getFunctionRepository();
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
		Function function = get(functionId);
		
		try {
			FunctionTokenHandle token;
			if(function.getClass().getSimpleName().equals("CompositeFunction")) {
				token = getFunctionClient().getLocalFunctionToken();
			} else {
				token = getFunctionClient().getFunctionToken();
			}
			try {
				ExecutionContext.setCurrentContext(createContext(getContext()));
				Input input = new Input();		
				String argument = executionInput.getArgument();
				if(argument!=null&&argument.length()>0) {
					input.setArgument(Json.createReader(new StringReader(argument)).readObject());				
				} else {
					input.setArgument(Json.createObjectBuilder().build());
				}
				
				input.setProperties(executionInput.getProperties());
				

				Output output = token.call(functionId, input);
				result.setOutput(output);
				
				List<AttachmentMeta> attachmentMetas = new ArrayList<>();
				result.setAttachments(attachmentMetas);
				if(output.getAttachments()!=null) {
					for(Attachment a:output.getAttachments()) {
						AttachmentMeta attachmentMeta;
						try {
							attachmentMeta = ReportNodeAttachmentManager.createAttachment(AttachmentHelper.hexStringToByteArray(a.getHexContent()), a.getName());
							attachmentMetas.add(attachmentMeta);
						} catch (AttachmentQuotaException e) {
							
						}
					}
				}
			} finally {
				token.release();
			}
		} catch(Exception e) {
			Output output = new Output();
			output.setError(e.getMessage());
			result.setOutput(output);
		}
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
		newFunction.getAttributes().put("name", "");
		return newFunction;
	}
}
