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
package step.plugins.scripteditor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.inject.Singleton;
import jakarta.ws.rs.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import step.core.deployment.AbstractStepServices;
import step.functions.manager.FunctionManager;
import step.functions.type.FunctionTypeRegistry;
import step.plugins.java.AbstractScriptFunctionType;
import step.plugins.java.GeneralScriptFunction;

@Singleton
@Path("/scripteditor")
@Tag(name = "Keyword editor")
public class ScriptEditorServices extends AbstractStepServices {

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
