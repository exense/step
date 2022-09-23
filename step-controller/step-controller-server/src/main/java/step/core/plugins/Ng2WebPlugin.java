package step.core.plugins;

public class Ng2WebPlugin extends AbstractWebPlugin {

	private String name;
	private String entryPoint;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEntryPoint() {
		return entryPoint;
	}

	public void setEntryPoint(String entryPoint) {
		this.entryPoint = entryPoint;
	}
}
