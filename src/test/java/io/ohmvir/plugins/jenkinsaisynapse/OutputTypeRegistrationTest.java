package io.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertFalse;

import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class OutputTypeRegistrationTest {
    @Test
    public void testOutputTypeRegistration(JenkinsRule j) {
        assertFalse(InputTypeRegistrationTest.getAllSubclasses(ModelOutput.class).stream()
                .anyMatch(subclass -> ModelRequest.getOutputTypesFromClass(subclass) == null));
        assertFalse(InputTypeRegistrationTest.getAllSubclasses(ModelOutput.class).stream()
                .anyMatch(subclass -> ModelRequest.getRequiredOutputCapabilities(subclass) == null));
        assertFalse(InputTypeRegistrationTest.getAllSubclasses(ModelOutput.class).stream()
                .anyMatch(subclass -> ModelRequest.getRequiredOutputCapabilities(subclass)
                                .isEmpty()
                        && ModelRequest.getOutputTypesFromClass(subclass).isEmpty()));
    }
}
