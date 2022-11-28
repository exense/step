package step.repositories.artifact;

import com.google.common.cache.CacheBuilder;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class RepositoryCacheTest {

    @Test
    public void testNoCache() {
        AtomicBoolean createWasCalled = new AtomicBoolean(false);
        AtomicBoolean updateWasCalled = new AtomicBoolean(false);
        AtomicBoolean deleteWasCalled = new AtomicBoolean(false);

        RepositoryCache<String> cache = new RepositoryCache<>(
                map -> {
                    createWasCalled.set(true);
                    return "";
                },
                (map, value) -> updateWasCalled.set(true),
                value -> deleteWasCalled.set(true)
        );

        Map<String, String> map = new HashMap<>();
        RepositoryCache.Entry<String> entry;

        entry = cache.lock(map);
        assert (createWasCalled.get());
        assert (!updateWasCalled.get());
        assert (!deleteWasCalled.get());
        assert (entry.getUsage() == 1);
        assert (cache.containsKey(map));

        createWasCalled.set(false);
        entry = cache.lock(map);
        assert (!createWasCalled.get());
        assert (updateWasCalled.get());
        assert (!deleteWasCalled.get());
        assert (entry.getUsage() == 2);

        updateWasCalled.set(false);
        entry = cache.release(map);
        assert (entry.getUsage() == 1);
        assert (!createWasCalled.get());
        assert (!updateWasCalled.get());
        assert (!deleteWasCalled.get());

        updateWasCalled.set(false);
        entry = cache.lock(map);
        assert (entry.getUsage() == 2);
        assert (!createWasCalled.get());
        assert (updateWasCalled.get());
        assert (!deleteWasCalled.get());

        cache.release(map);
        cache.release(map);
        assert (deleteWasCalled.get());
        // no long term cache:
        assert (!cache.containsKey(map));
        // nothing more to release:
        entry = cache.release(map);
        assert (entry == null);
    }

    @Test
    public void testCache() {
        AtomicBoolean createWasCalled = new AtomicBoolean(false);
        AtomicBoolean updateWasCalled = new AtomicBoolean(false);
        AtomicBoolean deleteWasCalled = new AtomicBoolean(false);

        RepositoryCache<String> cache = new RepositoryCache<>(
                map -> {
                    createWasCalled.set(true);
                    return "";
                },
                (map, value) -> updateWasCalled.set(true),
                value -> deleteWasCalled.set(true),
                // We add a cache
                CacheBuilder.newBuilder()
                        .maximumSize(1));

        RepositoryCache.Entry<String> entry;

        Map<String, String> map1 = new HashMap<>();
        map1.put("map", "1");
        cache.lock(map1);

        createWasCalled.set(false);
        Map<String, String> map2 = new HashMap<>();
        map2.put("map", "2");
        cache.lock(map2);
        assert (createWasCalled.get());

        entry = cache.release(map1);
        assert (entry.getUsage() == 0);
        // map1 is still in the long term cache:
        assert (!deleteWasCalled.get());
        assert (cache.containsKey(map1));

        entry = cache.release(map2);
        assert (entry.getUsage() == 0);
        // oldest key got evicted of the cache:
        assert (cache.containsKey(map2));
        assert (!cache.containsKey(map1));
        assert (deleteWasCalled.get());

        cache.lock(map2);
        assert (updateWasCalled.get());
    }
}
