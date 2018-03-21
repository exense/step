package step.core.deployment;

import java.io.IOException;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ExtendedUriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class CustomAuthenticationFilter extends AbstractServices implements ContainerRequestFilter, ClientResponseFilter {
	
	private static Logger logger = LoggerFactory.getLogger(CustomAuthenticationFilter.class);
	
	@Inject
	private ExtendedUriInfo extendendUriInfo;

	//@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		
		// Pick up a piece of information at filter time (from requestContext or by asking the IdP?)
		String info = "some info";
		
		AccessServices authenticationService = (AccessServices) controller.getContext().get(AccessServices.AUTHENTICATION_SERVICE);
		if (authenticationService != null) {
			// Communicate with the LoginProvider if needed?
			System.out.println(authenticationService.getLoginInformation(info));
		} else {
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
		}
		
		System.out.println("Dummy filter invoked.");
	}

	//@Override
	public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
		
	}
}
