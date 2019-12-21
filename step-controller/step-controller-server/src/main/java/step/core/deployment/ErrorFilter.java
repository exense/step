package step.core.deployment;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Secured
@Provider
public class ErrorFilter extends AbstractServices implements ExceptionMapper<Exception>  {

	private static final Logger logger = LoggerFactory.getLogger(ErrorFilter.class);
	
	@Override
	public Response toResponse(Exception exception) {
		logger.error("Unexpected error while processing request", exception);
		return Response.status(500).entity("Unexepected server error occurred: "+exception.getClass().getName()+":"+exception.getMessage()).type("text/plain").build();
	}
}