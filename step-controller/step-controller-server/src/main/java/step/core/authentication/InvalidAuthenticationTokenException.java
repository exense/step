package step.core.authentication;

/**
 * Thrown if an authentication token is invalid.
 *
 * @author cassiomolin
 */
public class InvalidAuthenticationTokenException extends RuntimeException {

    public InvalidAuthenticationTokenException(String message) {
        super(message);
    }
    
    public InvalidAuthenticationTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
