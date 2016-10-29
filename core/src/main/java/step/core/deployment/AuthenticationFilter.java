package step.core.deployment;

import java.io.IOException;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ExtendedUriInfo;

import step.core.access.Profile;

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter extends AbstractServices implements ContainerRequestFilter {
	
	@Inject
	private ExtendedUriInfo extendendUriInfo;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		Cookie sessionCookie = requestContext.getCookies().get("sessionid");
		if(sessionCookie!=null) {
			String token = sessionCookie.getValue();
			try {
				Session session = validateToken(token);
				requestContext.setProperty("session", session);
				
				Secured annotation = extendendUriInfo.getMatchedResourceMethod().getInvocable().getHandlingMethod().getAnnotation(Secured.class);
				String minRole = annotation.minRole();
				if(minRole.length()>0) {
					Profile profile = session.getProfile();
					
					boolean hasMinimumRole = (AccessServices.roleHierarchy.subList(AccessServices.roleHierarchy.indexOf(minRole),AccessServices.roleHierarchy.size()).indexOf(profile.getRole()))!=-1;
					
					if(!hasMinimumRole) {
						requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
					}
				}
			} catch (Exception e) {
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			}
		} else {
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
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
}