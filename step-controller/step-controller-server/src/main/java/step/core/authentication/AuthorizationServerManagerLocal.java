package step.core.authentication;

import io.jsonwebtoken.Jwts;
import step.framework.server.access.AccessManager;
import step.core.access.Role;
import step.framework.server.Session;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

public class AuthorizationServerManagerLocal implements AuthorizationServerManager {
    private final JWTSettings jwtSettings;
    private final AccessManager accessManager;

    public AuthorizationServerManagerLocal(JWTSettings jwtSettings, AccessManager accessManager) {
        this.jwtSettings = jwtSettings;
        this.accessManager = accessManager;
    }

    @Override
    public String issueToken(String username, Session session) {
        Role role = accessManager.getRoleInContext(session);
        String token = generateToken(username, role.getAttribute("name"),
                Date.from(ZonedDateTime.now().plusHours(8).toInstant()));
        session.setToken(AuthenticationFilter.AUTHENTICATION_SCHEME + " " + token);
        session.setLocalToken(true);
        return token;
    }

    @Override
    public String refreshToken(Session session) {
        Role role = accessManager.getRoleInContext(session);
        String token = generateToken(session.getUser().getSessionUsername(), role.getAttribute("name"),
                Date.from(ZonedDateTime.now().plusHours(8).toInstant()));
        session.setToken(AuthenticationFilter.AUTHENTICATION_SCHEME + " " + token);
        return token;
    }

    @Override
    public String getServiceAccountToken(Session session, long days) {
        Role role = accessManager.getRoleInContext(session);
        days = (days <= 0) ? 36500 : days;
        String token = generateToken(session.getUser().getSessionUsername(), role.getAttribute("name"),
                Date.from(ZonedDateTime.now().plusDays(days).toInstant()));
        return token;
    }

    private String generateToken(String username, String rolename, Date exp) {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setIssuer(jwtSettings.getIssuer())
                .setAudience(jwtSettings.getAudience())
                .setSubject(username)
                .setNotBefore(Date.from(ZonedDateTime.now().toInstant()))
                .setIssuedAt(Date.from(ZonedDateTime.now().toInstant()))
                .setExpiration(exp)
                //.claim("role", rolename)
                //.claim("refreshCount", 10)
                //.claim("refreshLimit", 1)
                .signWith(jwtSettings.getAlgo(), jwtSettings.getSecret())
                .compact();
    }
}
