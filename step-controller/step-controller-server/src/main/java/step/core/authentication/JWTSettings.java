package step.core.authentication;

import ch.exense.commons.app.Configuration;
import io.jsonwebtoken.SignatureAlgorithm;

public class JWTSettings {

    private static final String CONFIG_KEY_JWT_ALGO="authenticator.jwt.algo";
    private static final String CONFIG_KEY_JWT_CLOCKSKEW="authenticator.jwt.clock-skew";
    private static final String CONFIG_KEY_JWT_AUDIENCE="authenticator.jwt.audience";
    private static final String CONFIG_KEY_JWT_ISSUER="authenticator.jwt.issuer";
    private static final String CONFIG_KEY_JWT_ROLE_CLAIM_NAME ="authenticator.jwt.role-claim-name";
    private static final String CONFIG_KEY_JWT_REFRESH_COUNT_CLAIM_NAME="authenticator.jwt.refresh-count-claim-name";
    private static final String CONFIG_KEY_JWT_REFRESH_LIMIT_CLAIM_NAME="authenticator.jwt.refresh-limit-claim-name";
    private static final String CONFIG_KEY_JWT_ISSUER_CHECK="authenticator.jwt.issuer.check";
    private static final String CONFIG_KEY_JWT_AUDIENCE_CHECK="authenticator.jwt.audience.check";
    public static final String CONFIG_KEY_JWT_NOLOGIN="authenticator.jwt.no-login";
    
    private final SignatureAlgorithm algo;
    private final String secret;
    private final Long clockSkew;
    private final String audience;
    private final String issuer;
    private final String roleClaimName;
    private final String refreshCountClaimName;
    private final String refreshLimitClaimName;
    private final boolean checkIssuer;
    private final boolean checkAudience;
    

    public JWTSettings(Configuration configuration, String secret) {
        String algoStr = configuration.getProperty(CONFIG_KEY_JWT_ALGO,"HS256");
        algo = SignatureAlgorithm.valueOf(algoStr);
        this.secret = secret;
        clockSkew = configuration.getPropertyAsLong(CONFIG_KEY_JWT_CLOCKSKEW,10l);
        audience = configuration.getProperty(CONFIG_KEY_JWT_AUDIENCE,configuration.getProperty("controller.url"));
        issuer = configuration.getProperty(CONFIG_KEY_JWT_ISSUER,audience); //if not set the controller is also the issuer
        roleClaimName = configuration.getProperty(CONFIG_KEY_JWT_ROLE_CLAIM_NAME,"role");
        refreshCountClaimName = configuration.getProperty(CONFIG_KEY_JWT_ROLE_CLAIM_NAME,"refreshCount");
        refreshLimitClaimName = configuration.getProperty(CONFIG_KEY_JWT_ROLE_CLAIM_NAME,"refreshLimit");
        checkIssuer = configuration.getPropertyAsBoolean(CONFIG_KEY_JWT_ISSUER_CHECK, true);
        checkAudience = configuration.getPropertyAsBoolean(CONFIG_KEY_JWT_AUDIENCE_CHECK,true);
    }

    public SignatureAlgorithm getAlgo() {
        return algo;
    }

    public String getSecret() {
        return secret;
    }
    
    public Long getClockSkew() {
        return clockSkew;
    }

    public String getAudience() {
        return audience;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getRoleClaimName() {
        return roleClaimName;
    }

    public String getRefreshCountClaimName() {
        return refreshCountClaimName;
    }
    
    public String getRefreshLimitClaimName() {
        return refreshLimitClaimName;
    }

    public boolean isCheckIssuer() {
        return checkIssuer;
    }

    public boolean isCheckAudience() {
        return checkAudience;
    }

}
