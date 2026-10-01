package step.ide.exceptions;

// Exception to signal that a request is invalid, and is therefore reported to the client as a bad request
// rather than as an internal error.
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
