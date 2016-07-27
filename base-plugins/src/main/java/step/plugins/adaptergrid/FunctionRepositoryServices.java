package step.plugins.adaptergrid;

import java.io.StringReader;

import javax.json.Json;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.deployment.AbstractServices;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.functions.Function;
import step.functions.FunctionClient;
import step.functions.FunctionClient.FunctionToken;
import step.functions.FunctionRepository;
import step.functions.Input;
import step.functions.Output;

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
	@Path("/")
	public void execute(Function function) {
		getFunctionRepository().addFunction(function);
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/execute")
	public Output executeFunction(@PathParam("id") String functionId, String argument) {
		FunctionToken token = getFunctionClient().getLocalFunctionToken();
		try {
			ExecutionContext.setCurrentContext(createContext(getContext()));
			Input input = new Input();		
			input.setArgument(Json.createReader(new StringReader(argument)).readObject());
			return token.call(functionId, input);
		} finally {
			token.release();
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
	public void delete(@PathParam("id") String functionId) {
		getFunctionRepository().deleteFunction(functionId);
	}
	
	
}
