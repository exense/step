package step.plugins.functions.types;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.io.Files;

import step.commons.conf.Configuration;
import step.commons.processmanager.ManagedProcess;
import step.commons.processmanager.ManagedProcess.ManagedProcessException;
import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionType;

@FunctionType(name="selenium",label="Selenium")
public class SeleniumFunctionType extends AbstractFunctionType<SeleniumFunctionTypeConf> {

	@Override
	public String getHandlerChain(SeleniumFunctionTypeConf functionTypeConf) {
		return "class:step.handlers.scripthandler.ScriptHandler";
	}

	@Override
	public Map<String, String> getHandlerProperties(SeleniumFunctionTypeConf functionTypeConf) {
		Map<String, String> props = new HashMap<>();
		props.put("scripthandler.script.dir", functionTypeConf.scriptDir);
		return props;
	}

	@Override
	public SeleniumFunctionTypeConf newFunctionTypeConf() {
		SeleniumFunctionTypeConf conf = new SeleniumFunctionTypeConf();
		File scriptDir = new File(Configuration.getInstance().getProperty("controller.dir")+"/data/scripts");
		try {
			conf.setScriptDir(scriptDir.getCanonicalPath());
		} catch (IOException e) {
			// TODO handle IO error
		}			
		conf.setCallTimeout(180000);
		
		return conf;
	}

	@Override
	public void setupFunction(Function function) {
		File scriptDirFile = getScriptDir(function);
		File target = getScriptFile(function, scriptDirFile);
		if(scriptDirFile.exists() && scriptDirFile.isDirectory()) {
			File templateScript = new File(Configuration.getInstance().getProperty("controller.dir")+"/data/templates/kw_selenium.js");
			if(templateScript.exists()) {
				try {
					Files.copy(templateScript, target);
				} catch (IOException e) {
					
				}				
			}
		}
	}

	private File getScriptFile(Function function, File scriptDirFile) {
		File target = new File(scriptDirFile.getAbsolutePath()+"/"+function.getAttributes().get("name")+".js");
		return target;
	}

	private File getScriptDir(Function function) {
		SeleniumFunctionTypeConf conf = (SeleniumFunctionTypeConf) function.getConfiguration();
		String scriptDir = conf.getScriptDir();
		File scriptDirFile = new File(scriptDir);
		return scriptDirFile;
	}

	@Override
	public String getEditorPath(Function function) {
		File scriptDirFile = getScriptDir(function);
		File target = getScriptFile(function, scriptDirFile);
		try {
			ManagedProcess editor = new ManagedProcess("notepad.exe "+target.getAbsolutePath(), "Editor");
			editor.start();
		} catch (ManagedProcessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "root/functions";
	}

}
