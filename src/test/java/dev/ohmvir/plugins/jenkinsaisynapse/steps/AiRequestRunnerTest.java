package dev.ohmvir.plugins.jenkinsaisynapse.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
