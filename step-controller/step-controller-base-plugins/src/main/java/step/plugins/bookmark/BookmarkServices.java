package step.plugins.bookmark;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import step.controller.services.entities.AbstractEntityServices;
import step.core.entities.EntityManager;
import step.framework.server.security.SecuredContext;

@Singleton
@Path("bookmarks")
@Tag(name = "Bookmarks")
@Tag(name = "Entity=UserBookmark")
@SecuredContext(key = "entity", value = "bookmark", allowAllSignedInUsers = true) // all users/role should have rights
public class BookmarkServices extends AbstractEntityServices<UserBookmark> {
    public BookmarkServices() {
        super(EntityManager.bookmarks);
    }
}
