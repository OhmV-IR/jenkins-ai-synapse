package dev.ohmvir.plugins.jenkinsaisynapse.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.util.Timer;

/**
 * A thread-safe cache holding an immutable snapshot of data derived from configuration.
 * Readers always see a complete snapshot. A reload builds a brand new map and swaps it in,
 * so entries removed from the configuration disappear and edited entries are replaced.
 * If several reloads overlap, the one that started last wins, so a slow reload can never
 * overwrite the result of a newer one.
 */
public final class SnapshotCache<V> {
    private static final Logger LOGGER = Logger.getLogger(SnapshotCache.class.getName());

    private final String name;
    private final Supplier<Map<String, V>> loader;
    private final AtomicLong requestedGeneration = new AtomicLong();
    private long installedGeneration;
    private volatile Map<String, V> snapshot = Map.of();

    /**
     * @param name   A name for the cache, used in log messages.
     * @param loader Builds the full contents of the cache from the current configuration.
     *               It should not throw for a single bad entry, but leave that entry out.
     */
    public SnapshotCache(String name, Supplier<Map<String, V>> loader) {
        this.name = name;
        this.loader = loader;
    }

    /**
     * @return The current snapshot. It is immutable and never null.
     */
    public Map<String, V> get() {
        return snapshot;
    }

    /**
     * Rebuilds the cache from the current configuration on the calling thread and swaps it in.
     */
    public void reload() {
        long generation = requestedGeneration.incrementAndGet();
        Map<String, V> fresh = Collections.unmodifiableMap(new LinkedHashMap<>(loader.get()));
        synchronized (this) {
            if (generation > installedGeneration) {
                installedGeneration = generation;
                snapshot = fresh;
            }
        }
    }

    /**
     * Rebuilds the cache on a background thread. Failures are logged and never propagate.
     */
    public void reloadInBackground() {
        Timer.get().execute(() -> {
            try {
                reload();
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Failed to reload " + name, e);
            }
        });
    }
}
