package step.repositories.artifact;

import com.google.common.cache.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This cache associates a unique entry to a defined set of repository's parameters
 * This allows to track resources associated to a run
 * <p/>
 * This cache is based on:
 * - An HashMap, that keep in cache the entries currently used by at least one execution
 * - An optional com.google.common.cache.LoadingCache that keep track of the entries currently not used
 * <p/>
 * The constructor allows callbacks to create, update or delete the resources and manage the LoadingCache
 */
public class RepositoryCache<V> {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryCache.class);

    /**
     * Called when the entry is not in the cache and should be created
     */
    private final Function<Map<String, String>, V> createCacheEntry;
    /**
     * Called when the entry is locked, giving a chance to update the entry
     */
    private final BiConsumer<Map<String, String>, V> updateCacheEntry;
    /**
     * Called when the entry is removed from the cache and should be clean
     */
    private final Consumer<V> deleteCacheEntry;

    public static class Entry<V> {
        private int usage = 0;

        /**
         * Get the internal usage field.
         *
         * @return how many time {@code lock()} has been called without a corresponding call to {@code release()}
         */
        public int getUsage() {
            return usage;
        }

        private V value;

        /**
         * Retrieve the cache entry
         *
         * @return the cache entry
         */
        public V getValue() {
            return value;
        }
    }

    private LoadingCache<Map<String, String>, Entry<V>> longTermCache;
    private final ConcurrentHashMap<Map<String, String>, RepositoryCache.Entry<V>> currentlyLockedEntries = new ConcurrentHashMap<>();

    /**
     * Simple cache
     * <p>
     * Only keep the entries alive in cache during executions, calling deleteCacheEntry() when no execution with this map is running
     *
     * @param createCacheEntry the function used for creating an entry
     * @param updateCacheEntry called during a get, to ensure the entry is up to date
     * @param deleteCacheEntry cleanup the entry and remove the resources
     */
    public RepositoryCache(Function<Map<String, String>, V> createCacheEntry,
                           BiConsumer<Map<String, String>, V> updateCacheEntry,
                           Consumer<V> deleteCacheEntry) {
        this(createCacheEntry, updateCacheEntry, deleteCacheEntry, null);
    }

    /**
     * With external cache for longer term caching
     * <p>
     * Keep the entry in a cache constructed by the provided builder at the end of all executions related to this cache entry
     *
     * @param createCacheEntry the function used for creating an entry
     * @param updateCacheEntry called during a lock, to ensure the entry is up to date
     * @param deleteCacheEntry cleanup the entry and remove the resources
     * @param cacheBuilder will be used for caching entries in-between executions
     */
    public RepositoryCache(Function<Map<String, String>, V> createCacheEntry,
                           BiConsumer<Map<String, String>, V> updateCacheEntry,
                           Consumer<V> deleteCacheEntry,
                           CacheBuilder<Object, Object> cacheBuilder) {
        this.createCacheEntry = createCacheEntry;
        this.updateCacheEntry = updateCacheEntry;
        this.deleteCacheEntry = deleteCacheEntry;

        if (cacheBuilder != null) {
            longTermCache = cacheBuilder
                    .removalListener((RemovalListener<Map<String, String>, Entry<V>>) notification -> {
                        if (notification.getValue().usage == 0) {
                            logger.debug("Deleting the entry '"+notification.getKey()+"' from the long term cache");
                            deleteCacheEntry.accept(notification.getValue().getValue());
                        }
                    })
                    .build(new CacheLoader<>() {
                        @Override
                        public Entry<V> load(Map<String, String> key) throws UnsupportedOperationException {
                            throw new UnsupportedOperationException("This cache is supposed to be populated within " + RepositoryCache.class.getSimpleName());
                        }
                    });
        }
    }

    /**
     * Return the cache entry:
     * - call {@code createCacheEntry()} if the entry does not exist
     * - call {@code updateCacheEntry()} otherwise
     * <p>
     * The entry will be keep in cache until {@code release()} is called
     *
     * @param repositoryParameter the parameter associated with the entry
     * @return the cache entry
     */
    public Entry<V> lock(Map<String, String> repositoryParameter) {
        synchronized (this) {
            Entry<V> entry = get(repositoryParameter);
            entry.usage++;
            return entry;
        }

    }

    public Entry<V> get(Map<String, String> repositoryParameter) {
        Entry<V> entry = null;
        if (!currentlyLockedEntries.containsKey(repositoryParameter)) {
            if (longTermCache != null && ((entry = longTermCache.getIfPresent(repositoryParameter)) != null)) {
                logger.debug("Getting the entry '"+repositoryParameter+"' from the long term cache");
                updateCacheEntry.accept(repositoryParameter, entry.value);
            }
            if (entry == null) {
                entry = new Entry<>();
                logger.debug("Entry '"+repositoryParameter+"' is not in the cache, creating it");
                entry.value = createCacheEntry.apply(repositoryParameter);
            }
            currentlyLockedEntries.put(repositoryParameter, entry);
        } else {
            entry = currentlyLockedEntries.get(repositoryParameter);
            logger.debug("Getting the entry '"+repositoryParameter+"' from the locked entries");
            updateCacheEntry.accept(repositoryParameter, entry.value);
        }
        return entry;
    }

    /**
     * Decrease the corresponding cache entry's usage and put it in the long term cache if needed
     *
     * @param repositoryParameter: the parameter associated with the entry
     * @return The entry, null if it is not in the cache
     */
    public Entry<V> release(Map<String, String> repositoryParameter) {
        synchronized (this) {
            Entry<V> entry = currentlyLockedEntries.get(repositoryParameter);
            if (entry != null) {
                entry.usage--;

                if ((entry.usage) == 0) {
                    logger.debug("Removing the entry '"+repositoryParameter+"' from the the locked entries");
                    entry = currentlyLockedEntries.remove(repositoryParameter);
                    assert entry != null;
                    if (longTermCache != null) {
                        if (!longTermCache.asMap().containsKey(repositoryParameter)) {
                            logger.debug("Putting the entry '"+repositoryParameter+"' into the long term cache");
                            longTermCache.put(repositoryParameter, entry);
                        } else {
                            logger.debug("Entry '"+repositoryParameter+"' is already in the long term cache");
                        }
                    } else {
                        logger.debug("Deleting the entry '"+repositoryParameter+"', as no long term cache is available");
                        deleteCacheEntry.accept(entry.value);
                    }
                }
            }
            if (longTermCache != null) {
                // force cleanup:
                longTermCache.cleanUp();
            }
            return entry;
        }
    }

    /**
     * test if a key is present
     *
     * @param repositoryParameter the key
     * @return if an entry exists
     */
    public boolean containsKey(Map<String, String> repositoryParameter) {
        synchronized (this) {
            return (currentlyLockedEntries.containsKey(repositoryParameter) ||
                    ((longTermCache != null) && longTermCache.asMap().containsKey(repositoryParameter)));
        }
    }
}