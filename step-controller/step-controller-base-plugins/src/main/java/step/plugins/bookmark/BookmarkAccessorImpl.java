package step.plugins.bookmark;

import step.core.access.User;
import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;

import java.util.Map;

public class BookmarkAccessorImpl extends AbstractAccessor<UserBookmark> implements BookmarkAccessor {
    public BookmarkAccessorImpl(Collection<UserBookmark> collectionDriver) {
        super(collectionDriver);
    }

    @Override
    public void deleteUserBookmarks(User user) {
        this.findManyByCriteria(Map.of("userId", user.getId().toHexString())).forEach(b-> this.remove(b.getId()));
    }
}
