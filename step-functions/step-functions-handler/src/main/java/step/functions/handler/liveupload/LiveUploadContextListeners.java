package step.functions.handler.liveupload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class LiveUploadContextListeners {
    private static final Logger logger = LoggerFactory.getLogger(LiveUploadContextListeners.class);

    private static final Map<String, List<ListenerReference>> listenersMap = new ConcurrentHashMap<>();
    private static final ReferenceQueue<Consumer<LiveUpload>> referenceQueue = new ReferenceQueue<>();

    private static class ListenerReference extends WeakReference<Consumer<LiveUpload>> {
        final String contextId;

        ListenerReference(Consumer<LiveUpload> referent, String contextId, ReferenceQueue<Consumer<LiveUpload>> queue) {
            super(referent, queue);
            this.contextId = contextId;
        }
    }

    // Note that there is no need to unregister listeners, they are automatically removed once garbage-collected
    public static void registerListener(String contextId, Consumer<LiveUpload> callback) {
        Objects.requireNonNull(contextId);
        Objects.requireNonNull(callback);

        cleanupGarbageCollectedListeners();

        listenersMap.computeIfAbsent(contextId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new ListenerReference(callback, contextId, referenceQueue));
    }

    public static void notifyListeners(String contextId, LiveUpload upload) {
        cleanupGarbageCollectedListeners();

        List<ListenerReference> references = listenersMap.get(contextId);
        if (references == null) return;

        for (ListenerReference ref : references) {
            Consumer<LiveUpload> listener = ref.get();
            if (listener != null) {
                listener.accept(upload);
            }
        }

        removeStaleListenersForContext(contextId);
    }

    private static void cleanupGarbageCollectedListeners() {
        ListenerReference ref;
        while ((ref = (ListenerReference) referenceQueue.poll()) != null) {
            removeStaleListenersForContext(ref.contextId);
        }
    }

    private static void removeStaleListenersForContext(String contextId) {
        List<ListenerReference> list = listenersMap.get(contextId);
        if (list == null) return;

        AtomicInteger removed = new AtomicInteger(0);
        list.removeIf(ref -> ref.get() == null && removed.incrementAndGet() > 0);
        if (removed.get() > 0) {
            logger.debug("Removed {} stale listeners for context ID {} ", removed.get(), contextId);
        }
        if (list.isEmpty()) {
            listenersMap.remove(contextId);
            logger.debug("Removed stale context ID {}", contextId);
        }
    }
}
