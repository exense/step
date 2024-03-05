package step.plugins.bookmark;

import jakarta.validation.constraints.NotNull;
import step.core.accessors.AbstractOrganizableObject;

public class UserBookmark extends AbstractOrganizableObject {

    @NotNull
    private String userId;
    @NotNull
    private String url;

    public UserBookmark() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
