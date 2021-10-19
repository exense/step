package step.core.authentication;

import step.core.deployment.Session;

public interface AuthorizationServerManager {

    String issueToken(String username, Session session);

    String refreshToken(Session session);

    String getServiceAccountToken(Session session, long days);
}
