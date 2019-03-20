package step.core.deployment;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Secured
@Provider
public class ErrorFilter extends AbstractServices implements ExceptionMapper<Exception>  {

	@Override
	public Response toResponse(Exception exception) {
		exception.printStackTrace();
		return Response.status(500).entity("Unexepected server error occurred: "+exception.getClass().getName()+":"+exception.getMessage()).type("text/plain").build();
	}
}