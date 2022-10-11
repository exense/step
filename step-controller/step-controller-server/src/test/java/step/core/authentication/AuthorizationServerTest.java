package step.core.authentication;

import ch.exense.commons.app.Configuration;
import org.junit.Test;
import step.core.access.User;
import step.core.deployment.AuthenticationException;
import step.framework.server.access.AccessManager;
import step.core.access.Role;
import step.core.access.RoleResolver;
import step.framework.server.Session;

import java.util.Arrays;
import java.util.regex.Pattern;

public class AuthorizationServerTest {
    
    @Test
    public void jwtTests() throws AuthenticationException {
        JWTSettings jwtSettings = new JWTSettings(new Configuration(),"mysecret");
        AccessManager accessManager = new AccessManager() {
            @Override
            public void setRoleResolver(RoleResolver roleResolver) {
                
            }

            @Override
            public Role getRoleInContext(Session session) {
                Role role = new Role();
                role.addAttribute("name","admin");
                return role;
            }

            @Override
            public boolean checkRightInContext(Session session, String right) {
                return true;
            }
        };
        AuthorizationServerManager authorizationServerManager = new AuthorizationServerManagerLocal(jwtSettings, accessManager);
        ResourceServerManager resourceServerManager = new ResourceServerManager(jwtSettings, authorizationServerManager);
        Session session = new Session();
        User user = new User();
        user.setUsername("admin");
        session.setUser(user);
        String token = authorizationServerManager.getAccessToken(session, null, null);
        resourceServerManager.parseAndValidateToken(token, session);
                
        
        
    }

}
