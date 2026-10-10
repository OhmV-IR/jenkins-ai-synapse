package io.ohmvir.plugins.jenkinsaisynapse;

import static io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class ModelThinkingLevelTest {
    private static Optional<ModelThinkingLevel> resolve(ModelThinkingLevel requested, ModelThinkingLevel... supported) {
        return requested.resolveAgainst(List.of(supported));
    }

    @Test
    public void supportedLevelIsUsedAsIs() {
        assertEquals(Optional.of(MEDIUM), resolve(MEDIUM, LOW, MEDIUM, HIGH));
        assertEquals(Optional.of(OFF), resolve(OFF, OFF, ON));
    }

    @Test
    public void gradedLevelFallsBackToOnOnlyModels() {
        assertEquals(Optional.of(ON), resolve(MEDIUM, ON));
        assertEquals(Optional.of(ON), resolve(MAX, OFF, ON));
    }

    @Test
    public void gradedLevelMapsToNearestPreferringHigher() {
        assertEquals(Optional.of(HIGH), resolve(MEDIUM, LOW, HIGH));
        assertEquals(Optional.of(HIGH), resolve(MAX, LOW, HIGH));
        assertEquals(Optional.of(LOW), resolve(MEDIUM, LOW, MAX));
        assertEquals(Optional.of(LOW), resolve(LOW, ON, LOW, HIGH));
    }

    @Test
    public void onMapsToNearestMedium() {
        assertEquals(Optional.of(HIGH), resolve(ON, LOW, HIGH, MAX));
        assertEquals(Optional.of(LOW), resolve(ON, LOW));
    }

    @Test
    public void offIsSatisfiedByModelsThatDontThink() {
        assertEquals(Optional.of(OFF), resolve(OFF));
        assertEquals(Optional.of(LOW), resolve(OFF, LOW, HIGH));
        assertEquals(Optional.of(ON), resolve(OFF, ON));
    }

    @Test
    public void thinkingCantBeRequestedFromModelsThatDontThink() {
        assertEquals(Optional.empty(), resolve(ON));
        assertEquals(Optional.empty(), resolve(HIGH, OFF));
    }
}
