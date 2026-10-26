package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.ohmvir.plugins.jenkinsaisynapse.utils.SnapshotCache;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class SnapshotCacheTest {
    @Test
    public void replacesTheWholeSnapshotOnReload() {
        AtomicInteger calls = new AtomicInteger();
        SnapshotCache<String> cache = new SnapshotCache<>(
                "test", () -> calls.incrementAndGet() == 1 ? Map.of("a", "1", "b", "1") : Map.of("a", "2"));

        assertEquals(Map.of(), cache.get());
        cache.reload();
        assertEquals(Map.of("a", "1", "b", "1"), cache.get());
        cache.reload();
        assertEquals(Map.of("a", "2"), cache.get());
        assertThrows(UnsupportedOperationException.class, () -> cache.get().put("c", "3"));
    }

    @Test
    public void slowerOlderReloadDoesNotOverwriteNewerOne() throws Exception {
        CountDownLatch oldReloadStarted = new CountDownLatch(1);
        CountDownLatch releaseOldReload = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        SnapshotCache<String> cache = new SnapshotCache<>("test", () -> {
            if (calls.incrementAndGet() == 1) {
                oldReloadStarted.countDown();
                try {
                    releaseOldReload.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return Map.of("value", "old");
            }
            return Map.of("value", "new");
        });

        Thread oldReload = new Thread(cache::reload);
        oldReload.start();
        oldReloadStarted.await();
        cache.reload();
        releaseOldReload.countDown();
        oldReload.join();

        assertEquals(Map.of("value", "new"), cache.get());
    }
}
