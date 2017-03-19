package step.plugins.scripteditor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import step.commons.conf.Configuration;
import step.core.deployment.AbstractServices;
import step.functions.FunctionClient;
import step.functions.FunctionRepository;
import step.grid.Grid;
import step.plugins.adaptergrid.GridPlugin;
import step.plugins.functions.types.GeneralScriptFunction;
import step.plugins.functions.types.ScriptFunctionTypeHelper;

@Singleton
@Path("/scripteditor")
public class ScriptEditorServices extends AbstractServices {

	@GET
	@Path("/file/{filename}")
	public String getScript(@PathParam("filename") String filename) throws IOException {
		Grid grid = (Grid) getContext().get(GridPlugin.GRID_KEY);
		File scriptFIle = new File(Configuration.getInstance().getProperty("keywords.script.scriptdir")+"/"+filename);
		byte[] encoded = Files.readAllBytes(Paths.get(scriptFIle.toURI()));
		return new String(encoded, "UTF-8");
	}
	
	@POST
	@Path("/file/{filename}")
	public void saveScript(@PathParam("filename") String filename, String content) throws IOException {
		File scriptFile = new File(Configuration.getInstance().getProperty("keywords.script.scriptdir")+"/"+filename);
		Files.write(Paths.get(scriptFile.toURI()),content.getBytes("UTF-8"));
	}
	
	@POST
	@Path("/function/{functionid}/file")
	public void saveFunctionScript(@PathParam("functionid") String functionid, String content) throws IOException {
		File scriptFile = getScriptFile(functionid);
		Files.write(Paths.get(scriptFile.toURI()),content.getBytes("UTF-8"));
	}
	
	@GET
	@Path("/function/{functionid}/file")
	public String getFunctionScript(@PathParam("functionid") String functionid) throws IOException {
		File scriptFile = getScriptFile(functionid);
		
		byte[] encoded = Files.readAllBytes(Paths.get(scriptFile.toURI()));
		return new String(encoded, "UTF-8");
	}

	private File getScriptFile(String functionid) {
		FunctionClient functionClient = (FunctionClient) getContext().get(GridPlugin.FUNCTIONCLIENT_KEY);
		FunctionRepository functionRepository = functionClient.getFunctionRepository();
		
		GeneralScriptFunction function = (GeneralScriptFunction) functionRepository.getFunctionById(functionid);
		ScriptFunctionTypeHelper helper = new ScriptFunctionTypeHelper(getContext());
		File scriptFile = helper.getScriptFile(function);
		return scriptFile;
	}
}
