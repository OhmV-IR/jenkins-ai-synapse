package io.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class InputTypeRegistrationTest {

    public static <T> List<Class<T>> getAllSubclasses(Class<T> superClass) {
        try (ScanResult scanResult = new ClassGraph().enableClassInfo().scan()) {
            ClassInfoList subclasses = scanResult.getSubclasses(superClass.getName());
            return subclasses.loadClasses(superClass);
        }
    }

    @Test
    public void verifyAllInputClassesHaveType(JenkinsRule j) {
        assertFalse(getAllSubclasses(ModelInput.class).stream()
                .anyMatch(subclass -> ModelRequest.getInputTypesFromClass(subclass) == null));
        assertFalse(getAllSubclasses(ModelInput.class).stream()
                .anyMatch(subclass -> ModelRequest.getRequiredInputCapabilities(subclass) == null));
        assertFalse(getAllSubclasses(ModelInput.class).stream()
                .anyMatch(subclass ->
                        ModelRequest.getRequiredInputCapabilities(subclass).isEmpty()
                                && ModelRequest.getInputTypesFromClass(subclass).isEmpty()));
    }
}
