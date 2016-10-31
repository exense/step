package step.core.access;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ExtendedUriInfo;

import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;

@Secured
@Provider
public class AccessFilter extends AbstractServices implements ContainerResponseFilter {
	
	@Inject
	private ExtendedUriInfo extendendUriInfo;

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
	}
}