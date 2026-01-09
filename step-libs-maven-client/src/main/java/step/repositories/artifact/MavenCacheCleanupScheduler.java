package step.repositories.artifact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class MavenCacheCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MavenCacheCleanupScheduler.class);

    private static volatile MavenCacheCleanupScheduler instance;
    private static final Object lock = new Object();

    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, RepositoryCleanupConfig> registeredRepositories;

    public static class RepositoryCleanupConfig {
        private final File repositoryPath;
        private final Duration maxAge;
        private final Duration cleanupFrequency;
        private volatile boolean active;
        private volatile ScheduledFuture<?> scheduledTask;

        public RepositoryCleanupConfig(File repositoryPath, Duration maxAge, Duration cleanupFrequency) {
            this.repositoryPath = repositoryPath;
            this.maxAge = maxAge;
            this.cleanupFrequency = cleanupFrequency;
            this.active = true;
        }

        public File getRepositoryPath() { return repositoryPath; }
        public Duration getMaxAge() { return maxAge; }
        public Duration getCleanupFrequency() { return cleanupFrequency; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public ScheduledFuture<?> getScheduledTask() { return scheduledTask; }
        public void setScheduledTask(ScheduledFuture<?> scheduledTask) { this.scheduledTask = scheduledTask; }
    }

    private MavenCacheCleanupScheduler() {
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "maven-cache-cleanup");
            t.setDaemon(true);
            return t;
        });
        this.registeredRepositories = new ConcurrentHashMap<>();
    }

    public static MavenCacheCleanupScheduler getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new MavenCacheCleanupScheduler();
                }
            }
        }
        return instance;
    }

    public void registerRepository(File repositoryPath, Duration maxAge, Duration cleanupFrequency) {
        if (repositoryPath == null || !repositoryPath.exists() || !repositoryPath.isDirectory()) {
            logger.warn("Repository path is null, doesn't exist, or is not a directory: {}", repositoryPath);
            return;
        }

        String absolutePath = repositoryPath.getAbsolutePath();
        RepositoryCleanupConfig existingConfig = registeredRepositories.get(absolutePath);

        if (existingConfig != null) {
            logger.debug("Repository already registered: {}. Reactivating.", absolutePath);
            existingConfig.setActive(true);
        } else {
            RepositoryCleanupConfig config = new RepositoryCleanupConfig(repositoryPath, maxAge, cleanupFrequency);
            registeredRepositories.put(absolutePath, config);

            ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                () -> cleanupRepository(config),
                    cleanupFrequency.toMillis(), // Start immediately
                cleanupFrequency.toMillis(),
                TimeUnit.MILLISECONDS
            );
            config.setScheduledTask(task);

            logger.info("Registered repository for cleanup: {} (maxAge: {}, frequency: {})",
                absolutePath, maxAge, cleanupFrequency);
        }
    }

    public void unregisterRepository(File repositoryPath) {
        if (repositoryPath == null) {
            return;
        }

        String absolutePath = repositoryPath.getAbsolutePath();
        RepositoryCleanupConfig config = registeredRepositories.get(absolutePath);

        if (config != null) {
            config.setActive(false);
            ScheduledFuture<?> task = config.getScheduledTask();
            if (task != null) {
                task.cancel(false);
            }
            registeredRepositories.remove(absolutePath);
            logger.info("Unregistered repository from cleanup: {}", absolutePath);
        }
    }

    private void cleanupRepository(RepositoryCleanupConfig config) {
        if (!config.isActive()) {
            return;
        }

        try {
            File repoPath = config.getRepositoryPath();
            if (!repoPath.exists() || !repoPath.isDirectory()) {
                logger.warn("Repository path no longer exists or is not a directory: {}", repoPath);
                config.setActive(false);
                return;
            }

            long maxAgeMillis = config.getMaxAge().toMillis();
            long currentTime = System.currentTimeMillis();
            long cutoffTime = currentTime - maxAgeMillis;

            logger.debug("Starting cleanup for repository: {} (cutoff time: {})", repoPath, cutoffTime);

            int deletedItems = cleanupDirectoryRecursively(repoPath.toPath(), cutoffTime);

            if (deletedItems > 0) {
                logger.info("Cleanup completed for repository: {} - deleted {} items", repoPath, deletedItems);
            } else {
                logger.debug("Cleanup completed for repository: {} - no items to delete", repoPath);
            }

        } catch (Exception e) {
            logger.error("Error during repository cleanup: {}", config.getRepositoryPath(), e);
        }
    }

    private int cleanupDirectoryRecursively(Path directory, long cutoffTime) {
        int deletedCount = 0;

        try {
            if (!Files.exists(directory) || !Files.isDirectory(directory)) {
                return 0;
            }

            try (Stream<Path> children = Files.list(directory)) {
                List<Path> childList = children.collect(java.util.stream.Collectors.toList());

                for (Path child : childList) {
                    if (Files.isDirectory(child)) {
                        // Recursively clean subdirectories first
                        deletedCount += cleanupDirectoryRecursively(child, cutoffTime);

                        // After cleaning subdirectory, check if it's now empty and should be deleted
                        try (Stream<Path> remaining = Files.list(child)) {
                            if (remaining.count() == 0) {
                                if (Files.deleteIfExists(child)) {
                                    logger.debug("Deleted empty directory: {}", child);
                                    deletedCount++;
                                }
                            }
                        }
                    }
                }

                // After processing all subdirectories, check if this directory should be entirely deleted
                if (shouldDeleteDirectory(directory, cutoffTime)) {
                    deletedCount += deleteDirectoryContents(directory);
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to process directory: {}", directory, e);
        }

        return deletedCount;
    }

    private boolean shouldDeleteDirectory(Path directory, long cutoffTime) {
        try (Stream<Path> children = Files.list(directory)) {
            List<Path> childList = children.collect(java.util.stream.Collectors.toList());

            // Skip if directory is empty or contains only subdirectories
            List<Path> files = childList.stream()
                .filter(path -> !Files.isDirectory(path))
                .collect(java.util.stream.Collectors.toList());

            if (files.isEmpty()) {
                return false; // No files to check, don't delete
            }

            // Check if ALL files in this directory are old enough
            for (Path file : files) {
                if (!isFileOldEnough(file, cutoffTime)) {
                    return false; // At least one file is not old enough, don't delete
                }
            }

            logger.debug("All {} files in directory {} are old enough for deletion", files.size(), directory);
            return true; // All files are old enough

        } catch (Exception e) {
            logger.debug("Failed to check directory contents: {}", directory, e);
            return false;
        }
    }

    private int deleteDirectoryContents(Path directory) {
        int deletedCount = 0;

        try (Stream<Path> children = Files.list(directory)) {
            List<Path> childList = children.collect(java.util.stream.Collectors.toList());

            // Delete all files in this directory
            for (Path child : childList) {
                if (!Files.isDirectory(child)) {
                    if (Files.deleteIfExists(child)) {
                        logger.debug("Deleted file: {}", child);
                        deletedCount++;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to delete directory contents: {}", directory, e);
        }

        return deletedCount;
    }

    private boolean isFileOldEnough(Path file, long cutoffTime) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            long lastModified = Math.max(
                attrs.lastModifiedTime().toMillis(),
                attrs.lastAccessTime().toMillis()
            );
            return lastModified < cutoffTime;
        } catch (Exception e) {
            logger.debug("Failed to read file attributes: {}", file, e);
            return false;
        }
    }

    public void shutdown() {
        logger.info("Shutting down Maven cache cleanup scheduler");
        registeredRepositories.values().forEach(config -> {
            ScheduledFuture<?> task = config.getScheduledTask();
            if (task != null) {
                task.cancel(false);
            }
        });
        registeredRepositories.clear();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public int getRegisteredRepositoriesCount() {
        return (int) registeredRepositories.values().stream()
            .filter(RepositoryCleanupConfig::isActive)
            .count();
    }
}