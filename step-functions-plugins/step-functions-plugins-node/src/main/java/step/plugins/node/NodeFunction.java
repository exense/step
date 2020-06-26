package step.plugins.node;

import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.functions.Function;

public class NodeFunction extends Function {

	DynamicValue<String> jsfile = new DynamicValue<>("");

	@EntityReference(type=EntityManager.resources)
	public DynamicValue<String> getJsFile() {
		return jsfile;
	}

	public void setJsFile(DynamicValue<String> jsFile) {
		this.jsfile = jsFile;
	}
}
