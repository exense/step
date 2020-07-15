package step.core.access;

public class ExternalUser extends User {
	
	public ExternalUser() {
		super();
		super.setRole("admin");
		super.setPreferences(new Preferences());
	}
}
