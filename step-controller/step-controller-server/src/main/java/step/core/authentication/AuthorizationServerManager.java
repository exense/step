package step.core.authentication;

import io.jsonwebtoken.SigningKeyResolver;
import step.core.deployment.AuthenticationException;
import step.framework.server.Session;

public interface AuthorizationServerManager {

    String getAccessToken(Session session, String code, String session_state);

    String refreshToken(Session session) throws AuthenticationException;

    String getServiceAccountToken(Session session, long days);

    SigningKeyResolver getSigningKeyResolver();
}
