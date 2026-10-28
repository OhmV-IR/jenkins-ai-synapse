package dev.ohmvir.plugins.jenkinsaisynapse.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.EnvVars;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AiRequestStepExpansionTest {
    @Test
    public void dropsStopSequencesThatExpandToEmpty() {
        AiRequestStep step = new AiRequestStep("options");
        step.setExpandEnvVars(true);
        EnvVars env = new EnvVars();
        env.put("STOP", "END");
        env.put("NOTHING", "");
        assertEquals(List.of("END"), step.expandAll(env, List.of("${STOP}", "${NOTHING}")));
    }
}
