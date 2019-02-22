package step.resources;

public class CustomResourceType implements ResourceType {

	protected final boolean ephemeral;

	public CustomResourceType(boolean ephemeral) {
		super();
		this.ephemeral = ephemeral;
	}

	@Override
	public boolean isEphemeral() {
		return ephemeral;
	}
}
