package step.core.deployment;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class AuditResponseFilter extends AbstractServices implements ContainerResponseFilter {

	@Context
	private HttpServletRequest sr;
	
	@PostConstruct
	public void init() throws Exception {

	}

	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		AuditLogger.logResponse(sr, responseContext.getStatus());
	}
}
