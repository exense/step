package step.core.plugins;

import java.util.ArrayList;
import java.util.List;

public class WebPlugin {

	List<String> scripts = new ArrayList<>();
	
	List<String> angularModules = new ArrayList<>();

	public WebPlugin() {
		super();
	}

	public void setScripts(List<String> scripts) {
		this.scripts = scripts;
	}

	public void setAngularModules(List<String> angularModules) {
		this.angularModules = angularModules;
	}

	public List<String> getScripts() {
		return scripts;
	}

	public List<String> getAngularModules() {
		return angularModules;
	}
}
