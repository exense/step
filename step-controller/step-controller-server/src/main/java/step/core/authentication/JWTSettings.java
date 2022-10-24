package step.core.authentication;

import ch.exense.commons.app.Configuration;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class JWTSettings {

    private static final String CONFIG_KEY_JWT_ALGO="authenticator.jwt.algo";
    private static final String CONFIG_KEY_JWT_CLOCKSKEW="authenticator.jwt.clock-skew";

    private static final String CONFIG_KEY_JWT_CLIENT_ID="authenticator.jwt.client_id";
    private static final String CONFIG_KEY_JWT_AUDIENCE="authenticator.jwt.audience";
    private static final String CONFIG_KEY_JWT_ISSUER="authenticator.jwt.issuer";
    protected static final String CONFIG_KEY_JWT_ROLE_JSONPATH_PREFIX ="authenticator.jwt.roles.jsonpath.";

    protected static final String CONFIG_KEY_JWT_ROLE_ORDER ="authenticator.jwt.roles.order";
    private static final String CONFIG_KEY_USER_CLAIM_NAME ="authenticator.jwt.user-claim-name";
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
    private final Map<String,JsonPath> roleClaimJsonPathMap;
    private final String refreshCountClaimName;
    private final String refreshLimitClaimName;
    private final boolean checkIssuer;
    private final boolean checkAudience;
    private final JsonPath userClaimJsonPath;

    private final String clientId;


    public JWTSettings(Configuration configuration, String secret) {
        String audience1;
        String algoStr = configuration.getProperty(CONFIG_KEY_JWT_ALGO,"HS256");
        algo = SignatureAlgorithm.valueOf(algoStr);
        this.secret = secret;
        clockSkew = configuration.getPropertyAsLong(CONFIG_KEY_JWT_CLOCKSKEW,10l);
        audience1 = configuration.getProperty(CONFIG_KEY_JWT_AUDIENCE,configuration.getProperty("controller.url"));
        audience = Objects.requireNonNullElse(audience1, "local");
        issuer = configuration.getProperty(CONFIG_KEY_JWT_ISSUER,audience); //if not set the controller is also the issuer
        userClaimJsonPath = JsonPath.compile(configuration.getProperty(CONFIG_KEY_USER_CLAIM_NAME, "preferred_username"));//"sub");
        roleClaimJsonPathMap = parseRoleConfiguration(configuration);
        refreshCountClaimName = configuration.getProperty(CONFIG_KEY_JWT_REFRESH_COUNT_CLAIM_NAME,"refreshCount");
        refreshLimitClaimName = configuration.getProperty(CONFIG_KEY_JWT_REFRESH_LIMIT_CLAIM_NAME,"refreshLimit");
        checkIssuer = configuration.getPropertyAsBoolean(CONFIG_KEY_JWT_ISSUER_CHECK, true);
        checkAudience = configuration.getPropertyAsBoolean(CONFIG_KEY_JWT_AUDIENCE_CHECK,true);
        clientId = configuration.getProperty(CONFIG_KEY_JWT_CLIENT_ID,"step-local");
    }

    private Map<String, JsonPath> parseRoleConfiguration(Configuration configuration) {
        Map<String, JsonPath> results = new LinkedHashMap();
        //ordering (priority of roles) is required in case the user was assigned multiple Step roles in IDM.
        //Last role in the list has the highest priority, the produced linked hashmap is created with the highest priority as first element
        //The implementation handle role undefined in the priority list with be considered with the lowest priority
        List<String> order = List.of(configuration.getProperty(CONFIG_KEY_JWT_ROLE_ORDER, "guest,tester,developer,admin").split(","));
        configuration.getPropertyNames().stream().filter(n -> n.toString().startsWith(CONFIG_KEY_JWT_ROLE_JSONPATH_PREFIX)).map(p -> p.toString().replaceAll(CONFIG_KEY_JWT_ROLE_JSONPATH_PREFIX,""))
                .distinct().sorted(Comparator.comparingInt(order::indexOf)).collect(Collectors.toCollection(LinkedList::new)).descendingIterator().forEachRemaining(role -> {
                    JsonPath jsonPath = JsonPath.compile(configuration.getProperty(CONFIG_KEY_JWT_ROLE_JSONPATH_PREFIX + role));
                    results.put(role, jsonPath);
                });
        return results;
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

    public JsonPath getUserClaimJsonPath() {
        return userClaimJsonPath;
    }

    public Map<String, JsonPath> getRoleClaimJsonPathMap() {
        return roleClaimJsonPathMap;
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

    public String getClientId() {
        return clientId;
    }
}
