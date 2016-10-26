package step.core.deployment;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter extends AbstractServices implements ContainerRequestFilter {

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		Cookie sessionCookie = requestContext.getCookies().get("sessionid");
		if(sessionCookie!=null) {
			String token = sessionCookie.getValue();
			try {
				Session session = validateToken(token);
				requestContext.setProperty("session", session);
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