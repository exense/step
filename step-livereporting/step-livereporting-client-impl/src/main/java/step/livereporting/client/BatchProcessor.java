/*
 * Copyright (C) 2025, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

package step.livereporting.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * A generic batching processor that accumulates items and processes them in batches
 * based on size and time thresholds.
 *
 * @param <T> the type of items to batch
 */
public class BatchProcessor<T> implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(BatchProcessor.class);

    private final int batchSize;
    private final long flushIntervalMs;
    private final Consumer<List<T>> batchProcessor;
    private final String processorName;

    private final List<T> batch;
    private final ReentrantLock batchLock;
    private final ScheduledExecutorService scheduler;
    private volatile long lastFlushTime;

    /**
     * Creates a new BatchProcessor with the specified configuration.
     *
     * @param batchSize the maximum number of items to accumulate before processing
     * @param flushIntervalMs the maximum time in milliseconds to wait before processing
     * @param batchProcessor the function to process batches of items
     * @param processorName a name for this processor (used for logging and thread naming)
     */
    public BatchProcessor(int batchSize, long flushIntervalMs, Consumer<List<T>> batchProcessor, String processorName) {
        this.batchSize = batchSize;
        this.flushIntervalMs = flushIntervalMs;
        this.batchProcessor = batchProcessor;
        this.processorName = processorName != null ? processorName : "batch-processor";

        this.batch = new ArrayList<>(batchSize);
        this.batchLock = new ReentrantLock();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, this.processorName + "-flusher");
            t.setDaemon(true);
            return t;
        });
        this.lastFlushTime = System.currentTimeMillis();

        this.scheduler.scheduleAtFixedRate(this::flushIfNeeded, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Adds an item to the batch. If the batch reaches the configured size,
     * it will be processed immediately.
     *
     * @param item the item to add to the batch
     */
    public void add(T item) {
        batchLock.lock();
        try {
            batch.add(item);
            if (batch.size() >= batchSize) {
                flushBatch();
            }
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * Forces processing of any items currently in the batch, regardless of size or time thresholds.
     */
    public void flush() {
        batchLock.lock();
        try {
            if (!batch.isEmpty()) {
                flushBatch();
            }
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * Gets the current number of items in the batch waiting to be processed.
     *
     * @return the current batch size
     */
    public int getCurrentBatchSize() {
        batchLock.lock();
        try {
            return batch.size();
        } finally {
            batchLock.unlock();
        }
    }

    private void flushIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFlushTime >= flushIntervalMs) {
            batchLock.lock();
            try {
                if (!batch.isEmpty()) {
                    flushBatch();
                }
            } finally {
                batchLock.unlock();
            }
        }
    }

    private void flushBatch() {
        if (batch.isEmpty()) {
            return;
        }

        List<T> toProcess = new ArrayList<>(batch);
        batch.clear();
        lastFlushTime = System.currentTimeMillis();

        try {
            batchProcessor.accept(toProcess);
            logger.debug("Successfully processed batch of {} items using {}", toProcess.size(), processorName);
        } catch (Exception e) {
            logger.error("Failed to process batch of {} items using {}", toProcess.size(), processorName, e);
            throw e;
        }
    }

    @Override
    public void close() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        batchLock.lock();
        try {
            if (!batch.isEmpty()) {
                flushBatch();
            }
        } finally {
            batchLock.unlock();
        }
    }
}