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
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ExtendedUriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.access.Profile;

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class DummyAuthenticationFilter extends AbstractServices implements ContainerRequestFilter, ClientResponseFilter {
	
	private static Logger logger = LoggerFactory.getLogger(DummyAuthenticationFilter.class);
	
	@Inject
	private ExtendedUriInfo extendendUriInfo;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		// do nothing
		System.out.println("Dummy filter invoked.");
	}

	@Override
	public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
		
	}
}