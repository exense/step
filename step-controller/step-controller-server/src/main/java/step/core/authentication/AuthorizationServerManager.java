package step.core.authentication;

import io.jsonwebtoken.SigningKeyResolver;
import jakarta.ws.rs.container.ContainerRequestContext;
import step.core.deployment.AuthenticationException;
import step.framework.server.Session;

public interface AuthorizationServerManager {

    String issueToken(String username, Session session);

    String refreshToken(Session session);

    String getServiceAccountToken(Session session, long days);

    boolean filter(ContainerRequestContext requestContext, Session session);

    SigningKeyResolver getSigningKeyResolver();
}
