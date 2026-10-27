package dev.ohmvir.plugins.jenkinsaisynapse.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

public class AiRequestRunnerTest {
    @Test
    public void providerTimeoutBoundsTheRequestWithoutAStepTimeout() {
        assertEquals(300, AiRequestRunner.effectiveTimeout(300, 0));
        assertEquals(0, AiRequestRunner.effectiveTimeout(0, 0));
    }

    @Test
    public void stepTimeoutReplacesTheProviderTimeout() {
        assertEquals(1800, AiRequestRunner.effectiveTimeout(300, 1800));
        assertEquals(60, AiRequestRunner.effectiveTimeout(300, 60));
        assertEquals(60, AiRequestRunner.effectiveTimeout(0, 60));
    }

    @Test
    public void timeoutMessageNamesTheLimitThatApplied() {
        String stepMessage = AiRequestRunner.timeoutMessage(300, 1800);
        assertTrue(stepMessage.startsWith("AI request timed out after 1800 seconds"), stepMessage);
        assertTrue(stepMessage.contains("the step's timeout of 1800 seconds"), stepMessage);

        String providerMessage = AiRequestRunner.timeoutMessage(300, 0);
        assertTrue(providerMessage.startsWith("AI request timed out after 300 seconds"), providerMessage);
        assertTrue(providerMessage.contains("the provider's timeout of 300 seconds"), providerMessage);
    }

    @Test
    public void shutDownWaitsForTheRequestThreadToStop() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicBoolean finished = new AtomicBoolean();
        CountDownLatch started = new CountDownLatch(1);
        executor.submit(() -> {
            started.countDown();
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                // Takes a moment to clean up after being interrupted
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                    // Nothing more to do
                }
                finished.set(true);
            }
        });
        started.await();
        ByteArrayOutputStream log = new ByteArrayOutputStream();

        AiRequestRunner.shutDownAndAwait(executor, new PrintStream(log, true, StandardCharsets.UTF_8), 10);

        assertTrue(finished.get());
        assertTrue(executor.isTerminated());
        assertFalse(log.toString(StandardCharsets.UTF_8).contains("may still be running"));
    }

    @Test
    public void shutDownWarnsWhenATaskDoesNotStop() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicBoolean release = new AtomicBoolean();
        CountDownLatch started = new CountDownLatch(1);
        // Does not respond to interruption, like a tool call that is blocked in uninterruptible code
        executor.submit(() -> {
            started.countDown();
            while (!release.get()) {
                Thread.onSpinWait();
            }
        });
        started.await();
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        try {
            AiRequestRunner.shutDownAndAwait(executor, new PrintStream(log, true, StandardCharsets.UTF_8), 1);

            String output = log.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("did not stop within 1 seconds"), output);
            assertTrue(output.contains("may still be running"), output);
        } finally {
            release.set(true);
        }
    }

    @Test
    public void shutDownKeepsTheInterruptFlagWhenInterruptedWhileWaiting() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        Thread.currentThread().interrupt();
        try {
            AiRequestRunner.shutDownAndAwait(executor, new PrintStream(log, true, StandardCharsets.UTF_8), 10);

            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
