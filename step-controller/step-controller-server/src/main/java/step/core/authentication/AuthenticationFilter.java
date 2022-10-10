package step.core.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.access.User;
import step.core.access.UserAccessor;
import step.core.controller.errorhandling.ApplicationException;
import step.core.deployment.AbstractStepServices;
import step.framework.server.security.Secured;
import step.framework.server.Session;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Objects;

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter extends AbstractStepServices implements ContainerRequestFilter {

    private static Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private static final String REALM = "step";
    public static String AUTHENTICATION_SCHEME = "Bearer";
    private AuthorizationServerManager authorizationServerManager;
    private ResourceServerManager resourceServerManager;
    private UserAccessor userAccessor;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        
        authorizationServerManager = context.get(AuthorizationServerManager.class);
        resourceServerManager = context.get(ResourceServerManager.class);
        userAccessor = context.getUserAccessor();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Get the Authorization header from the request header or http session
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        authorizationHeader = (authorizationHeader != null)  ? authorizationHeader : 
                getAuthorizationTokenFromSession();


        // Validate the Authorization header
        if (isTokenBasedAuthentication(authorizationHeader)) {
            // Extract the token from the Authorization header
            String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();
            try {
                // Validate and parse the token
                AuthenticationTokenDetails authenticationTokenDetails = resourceServerManager.parseAndValidateToken(token, getSession());
                initSessionIfRequired(authenticationTokenDetails);
            } catch (Exception e) {
                logger.error("Authentication failed",e);
                abortWithUnauthorized(requestContext);
            }   
        } else {
            abortWithUnauthorized(requestContext);
            return;
        }
    }


    private void initSessionIfRequired(AuthenticationTokenDetails authenticationTokenDetails) {
        //If step session is not yet set or not really initiated
        if (getSession() == null || getSession().getUser() == null || getSession().getUser().getUsername() == null){
            Session session = Objects.requireNonNullElse(getSession(),new Session());
            String username = authenticationTokenDetails.getUsername();
            User user = userAccessor.getByUsername(username);
            if(user == null) {
                throw new ApplicationException(100, "Unknow user '"+username+"': this user is not defined in step", null);
            }
            session.setAuthenticated(true);
            session.setLocalToken(false);
            session.setUser(user);
            setSession(session);
        }
    }

    private String getAuthorizationTokenFromSession() {
        Session session = getSession();
        String token = null;
        if (session!=null) {
            token = session.getToken();
        }
        return token;
    }


    private boolean isTokenBasedAuthentication(String authorizationHeader) {
        // Check if the Authorization header is valid
        // It must not be null and must be prefixed with "Bearer" plus a whitespace
        // The authentication scheme comparison must be case-insensitive
        return authorizationHeader != null && authorizationHeader.toLowerCase()
                .startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext) {
        // Abort the filter chain with a 401 status code response
        // The WWW-Authenticate header is sent along with the response
        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .header(HttpHeaders.WWW_AUTHENTICATE,
                                AUTHENTICATION_SCHEME + " realm=\"" + REALM + "\"")
                        .build());
    }
}
