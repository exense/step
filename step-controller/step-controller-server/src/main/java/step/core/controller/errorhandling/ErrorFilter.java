package step.core.controller.errorhandling;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;

@Secured
@Provider
public class ErrorFilter extends AbstractServices implements ExceptionMapper<Exception>  {

	private static final Logger logger = LoggerFactory.getLogger(ErrorFilter.class);
	
	@Override
	public Response toResponse(Exception exception) {
		if(exception instanceof ApplicationException) {
			return Response.status(Response.Status.BAD_REQUEST).entity(((ApplicationException) exception).getErrorMessage()).type("text/plain").build();
		} else {
			logger.error("Unexpected error while processing request", exception);
			return Response.status(500).entity("Unexepected server error occurred: "+exception.getClass().getName()+":"+exception.getMessage()).type("text/plain").build();
		}
	}
}