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
public class AuthenticationFilter extends AbstractServices implements ContainerRequestFilter, ClientResponseFilter {
	
	private static Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
	
	@Inject
	private ExtendedUriInfo extendendUriInfo;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		boolean useAuthentication = AccessServices.useAuthentication();
		if(useAuthentication) {
			Cookie sessionCookie = requestContext.getCookies().get("sessionid");
			if(sessionCookie!=null) {
				String token = sessionCookie.getValue();
				try {
					Session session = validateToken(token);
					requestContext.setProperty("session", session);
					
					Secured annotation = extendendUriInfo.getMatchedResourceMethod().getInvocable().getHandlingMethod().getAnnotation(Secured.class);
					String right = annotation.right();
					if(right.length()>0) {
						Profile profile = session.getProfile();
						
						boolean hasRight = profile.getRights().contains(right);
						
						if(!hasRight) {
							requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
						}
					}
				} catch (Exception e) {
					logger.error("An exception was thrown while checking user rights.", e);
					requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
				}
			} else {
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			}
		} else {
			requestContext.setProperty("session", AccessServices.ANONYMOUS_SESSION);
		}
	}

	private Session validateToken(String token) throws Exception {
		AccessServices authenticationService = (AccessServices) controller.getContext().get(AccessServices.AUTHENTICATION_SERVICE);
		if (authenticationService != null) {
			Session session = authenticationService.validateAndTouchToken(token);
			if (session != null) {
				return session;
			} else {
				throw new Exception("Session with token '" + token + "' is invalid");
			}
		} else {
			throw new Exception("authenticationService is null");
		}
	}

	@Override
	public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
		
	}
}