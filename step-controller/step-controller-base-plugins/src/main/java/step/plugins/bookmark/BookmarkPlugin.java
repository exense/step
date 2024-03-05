package step.plugins.bookmark;

import step.core.GlobalContext;
import step.core.access.User;
import step.core.access.UserAccessor;
import step.core.collections.Collection;
import step.core.collections.filters.Equals;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;

import java.util.function.Function;

@Plugin
public class BookmarkPlugin extends AbstractControllerPlugin {

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);
        UserAccessor userAccessor = context.getUserAccessor();
        Collection<UserBookmark> bookmarkCollection = context.getCollectionFactory().getCollection(EntityManager.bookmarks, UserBookmark.class);
        BookmarkAccessorImpl bookmarkAccessor = new BookmarkAccessorImpl(bookmarkCollection);
        context.put(BookmarkAccessor.class, bookmarkAccessor);
        context.getEntityManager().register(new Entity<>(EntityManager.bookmarks, bookmarkAccessor, UserBookmark.class));
        context.get(TableRegistry.class).register(EntityManager.bookmarks,
                new Table<>(bookmarkCollection, null, false)
                        .withTableFiltersFactory((tableParameters, session) -> {
                            String userId = session.getUser().getId().toHexString();
                            return new Equals("userId", userId);
                        }));
        userAccessor.registerOnRemoveHook(new Function<User, Void>() {
            @Override
            public Void apply(User user) {
                bookmarkAccessor.deleteUserBookmarks(user);
                return null;
            }
        });

        context.getServiceRegistrationCallback().registerService(BookmarkServices.class);
    }
}
