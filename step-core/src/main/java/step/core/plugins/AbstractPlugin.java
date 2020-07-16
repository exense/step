package step.core.plugins;

public class AbstractPlugin implements OptionalPlugin {

	public AbstractPlugin() {
		super();
	}

	@Override
	public boolean validate() {
		return true;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}