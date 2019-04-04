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

import step.core.deployment.AbstractServices;
import step.functions.manager.FunctionManager;
import step.functions.type.FunctionTypeRegistry;
import step.plugins.java.AbstractScriptFunctionType;
import step.plugins.java.GeneralScriptFunction;

@Singleton
@Path("/scripteditor")
public class ScriptEditorServices extends AbstractServices {

	@GET
	@Path("/file/{filename}")
	public String getScript(@PathParam("filename") String filename) throws IOException {
		File scriptFIle = new File(configuration.getProperty("keywords.script.scriptdir")+"/"+filename);
		byte[] encoded = Files.readAllBytes(Paths.get(scriptFIle.toURI()));
		return new String(encoded, "UTF-8");
	}
	
	@POST
	@Path("/file/{filename}")
	public void saveScript(@PathParam("filename") String filename, String content) throws IOException {
		File scriptFile = new File(configuration.getProperty("keywords.script.scriptdir")+"/"+filename);
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private File getScriptFile(String functionid) {
		FunctionManager functionManager = getContext().get(FunctionManager.class);
		GeneralScriptFunction function = (GeneralScriptFunction) functionManager.getFunctionById(functionid);
		
		FunctionTypeRegistry functionTypeRegistry = getContext().get(FunctionTypeRegistry.class);
		return ((AbstractScriptFunctionType)functionTypeRegistry.getFunctionTypeByFunction(function)).getScriptFile(function);
	}
}
