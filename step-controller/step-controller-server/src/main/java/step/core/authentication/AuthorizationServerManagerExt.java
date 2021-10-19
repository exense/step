package step.core.authentication;

import step.core.access.AccessManager;
import step.core.deployment.Session;

public class AuthorizationServerManagerExt implements AuthorizationServerManager {
    private final JWTSettings jwtSettings;
    private final AccessManager accessManager;

    public AuthorizationServerManagerExt(JWTSettings jwtSettings, AccessManager accessManager) {
        this.jwtSettings = jwtSettings;
        this.accessManager = accessManager;
    }
    
    public String issueToken(String username, Session session) {
        throw new RuntimeException("issue token for external provider is not implemented");
    }

    public String refreshToken(Session session) {
        throw new RuntimeException("refresh token for external provider is not implemented");
    }

    @Override
    public String getServiceAccountToken(Session session ,long days) {
        throw new RuntimeException("generate token for external provider is not implemented");
    }

}
