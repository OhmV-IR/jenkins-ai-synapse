package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutputKind;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

public class OutputTypeRegistrationTest {
    @Test
    public void verifyAllOutputClassesDeclareAKind() throws Exception {
        var outputClasses = InputTypeRegistrationTest.getAllSubclasses(ModelOutput.class);
        assertFalse(outputClasses.isEmpty());
        for (Class<? extends ModelOutput> outputClass : outputClasses) {
            Field kindField = outputClass.getField("KIND");
            assertTrue(Modifier.isStatic(kindField.getModifiers()), outputClass.getName() + ".KIND should be static");
            ModelOutputKind<?> kind = (ModelOutputKind<?>) kindField.get(null);
            assertEquals(outputClass, kind.type(), outputClass.getName() + ".KIND describes the wrong class");
            assertFalse(
                    kind.requiredCapabilities().isEmpty()
                            && kind.requiredOutputTypes().isEmpty(),
                    outputClass.getName() + " should require a capability or an output type");
        }
    }
}
