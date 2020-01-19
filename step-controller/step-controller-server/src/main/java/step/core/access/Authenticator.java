package step.core.access;

public interface Authenticator {

	public boolean authenticate(Credentials credentials);
}
