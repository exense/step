package step.plugins.docker;

import com.fasterxml.jackson.annotation.JsonProperty;
import step.core.accessors.AbstractIdentifiableObject;

public class DockerRegistryConfiguration extends AbstractIdentifiableObject {
    @JsonProperty
    private String url;
    @JsonProperty
    private String username;
    @JsonProperty
    private String password;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
