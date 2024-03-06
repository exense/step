package step.core.deployment;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import step.framework.server.Session;
import step.framework.server.access.TokenType;

import java.io.IOException;


/**
 * First filter to be executed, initialize Session when required
 */
@Provider
@Priority(1) //Run first
public class SessionFilter extends AbstractStepServices implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        retrieveOrInitializeSession();
    }

    protected Session retrieveOrInitializeSession() {
        Session session = getSession();
        if(session == null) {
            session = new Session();
            session.setAuthenticated(false); //no authentication here, only setting
            session.setTokenType(TokenType.LOCAL_UI_TOKEN);//default
            setSession(session);
        }
        return session;
    }
}
