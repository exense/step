package step.core.authentication;

import java.time.ZonedDateTime;

public class AuthenticationTokenDetails {
    private final String id;
    private final String username;
    private final String role;
    private final String issuer;
    private final String audience;
    private final ZonedDateTime issuedDate;
    private final ZonedDateTime notBeforeDate;
    private final ZonedDateTime expirationDate;
    private final int refreshCount;
    private final int refreshLimit;

    private AuthenticationTokenDetails(String id, String username, String role, String issuer,
                                       String audience, ZonedDateTime notBeforeDate, ZonedDateTime issuedDate, ZonedDateTime expirationDate, int refreshCount, int refreshLimit) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.issuer = issuer;
        this.audience = audience;
        this.notBeforeDate = notBeforeDate;
        this.issuedDate = issuedDate;
        this.expirationDate = expirationDate;
        this.refreshCount = refreshCount;
        this.refreshLimit = refreshLimit;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getAudience() {
        return audience;
    }

    public ZonedDateTime getNotBeforeDate() {
        return notBeforeDate;
    }

    public ZonedDateTime getIssuedDate() {
        return issuedDate;
    }

    public ZonedDateTime getExpirationDate() {
        return expirationDate;
    }

    public int getRefreshCount() {
        return refreshCount;
    }

    public int getRefreshLimit() {
        return refreshLimit;
    }
    

    /**
     * Check if the authentication token is eligible for refreshment.
     *
     * @return
     */
    public boolean isEligibleForRefreshment() {
        return refreshCount < refreshLimit;
    }

    /**
     * Builder for the {@link AuthenticationTokenDetails}.
     */
    public static class Builder {

        private String id;
        private String username;
        private String role;
        private String issuer;
        private String audience;
        private ZonedDateTime notBeforeDate;
        private ZonedDateTime issuedDate;
        private ZonedDateTime expirationDate;
        private int refreshCount;
        private int refreshLimit;
        

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withRole(String role) {
            this.role =  role;
            return this;
        }

        public Builder withIssuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder withAudience(String audience) {
            this.audience = audience;
            return this;
        }


        public Builder withNotBeforeDate(ZonedDateTime notBeforeDate) {
            this.notBeforeDate = notBeforeDate;
            return this;
        }

        public Builder withIssuedDate(ZonedDateTime issuedDate) {
            this.issuedDate = issuedDate;
            return this;
        }

        public Builder withExpirationDate(ZonedDateTime expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public Builder withRefreshCount(int refreshCount) {
            this.refreshCount = refreshCount;
            return this;
        }

        public Builder withRefreshLimit(int refreshLimit) {
            this.refreshLimit = refreshLimit;
            return this;
        }

        public AuthenticationTokenDetails build() {
            return new AuthenticationTokenDetails(id, username, role, issuer, audience, notBeforeDate, issuedDate, expirationDate, refreshCount, refreshLimit);
        }

    }

    @Override
    public String toString() {
        return "AuthenticationTokenDetails{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", issuer='" + issuer + '\'' +
                ", audience='" + audience + '\'' +
                ", issuedDate=" + issuedDate +
                ", notBeforeDate=" + notBeforeDate +
                ", expirationDate=" + expirationDate +
                ", refreshCount=" + refreshCount +
                ", refreshLimit=" + refreshLimit +
                '}';
    }
}
