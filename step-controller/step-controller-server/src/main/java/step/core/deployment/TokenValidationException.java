package step.core.deployment;

public class TokenValidationException extends AuthenticationException {

	private static final long serialVersionUID = 1609024448637213042L;

	public TokenValidationException(String msg) {
		super(msg);
	}
}
