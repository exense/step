package step.core.authentication;

import ch.exense.commons.app.Configuration;
import org.junit.Test;
import step.core.access.AccessManager;
import step.core.access.AccessManagerImpl;
import step.core.access.Role;
import step.core.access.RoleResolver;
import step.core.deployment.Session;

public class AuthorizationServerTest {
    
    @Test
    public void jwtTests(){
        JWTSettings jwtSettings = new JWTSettings(new Configuration());
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
        String token = authorizationServerManager.issueToken("myUser", session);
        resourceServerManager.parseAndValidateToken(token, session);
                
        
        
    }
}
