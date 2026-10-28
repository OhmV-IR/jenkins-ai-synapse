package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.*;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class InputTypeRegistrationTest {

    public static <T> List<Class<? extends T>> getAllSubclasses(Class<T> superClass) {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableClassLoaders(superClass.getClassLoader())
                .acceptPackages("dev.ohmvir.plugins.jenkinsaisynapse.api")
                .scan()) {
            List<Class<? extends T>> subclasses = new ArrayList<>();
            for (var info : scanResult.getAllSubclasses(superClass)) {
                if (!info.isAbstract()) {
                    try {
                        subclasses.add(Class.forName(info.getName(), false, superClass.getClassLoader())
                                .asSubclass(superClass));
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("Could not load scanned subclass " + info.getName(), e);
                    }
                }
            }
            return subclasses;
        }
    }

    @Test
    public void verifyAllInputClassesDeclareRequirements() throws Exception {
        List<ModelInput> samples = List.of(
                new InputAudioContent(new byte[0], "audio/wav"),
                new InputConversationContent(new ModelConversation()),
                new InputFileContent("file.txt", new byte[0], "text/plain"),
                new InputImageContent(new byte[0], "image/png"),
                new InputSkillContent(null),
                new InputTextContent("text"),
                new InputToolContent(null),
                new InputVideoContent(new byte[0], "video/mp4"),
                new InfillPrefixContent("prefix"),
                new InfillSuffixContent("suffix"),
                new DecisionChoiceQuestionContent("choice", null, Map.of("a", "A", "b", "B")),
                new DecisionScoreQuestionContent("score", null, List.of("low", "high")),
                new DecisionYesNoQuestionContent("yesNo", "Is this true?"),
                new MaxOutputTokensContent(1),
                new StopSequencesContent(List.of("stop")),
                new SystemPromptContent("system"),
                new TemperatureContent(1),
                new ThinkingLevelContent(ModelThinkingLevel.ON),
                new ToolCallResponseContent("id", true, "response", null),
                new TopKContent(1),
                new TopPContent(1));

        Set<Class<?>> sampled = samples.stream().map(Object::getClass).collect(Collectors.toSet());
        assertEquals(Set.copyOf(getAllSubclasses(ModelInput.class)), sampled, "Add new input types to this test");
        for (ModelInput sample : samples) {
            assertFalse(
                    sample.getRequiredCapabilities().isEmpty()
                            && sample.getRequiredInputTypes().isEmpty(),
                    sample.getClass().getName() + " should require a capability or an input type");
        }
    }
}
