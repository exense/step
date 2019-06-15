package step.core.plugins;

public class AbstractPlugin {

	public AbstractPlugin() {
		super();
	}

	public boolean validate() {
		return true;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}