package io.ohmvir.plugins.jenkinscr;

import hudson.model.Describable;
import io.ohmvir.plugins.jenkinscr.configuration.client.*;
import io.ohmvir.plugins.jenkinscr.configuration.models.*;
import io.ohmvir.plugins.jenkinscr.configuration.prompts.FullPrompt;
import io.ohmvir.plugins.jenkinscr.configuration.prompts.PromptConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.prompts.SimplePrompt;
import io.ohmvir.plugins.jenkinscr.configuration.skills.*;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
public class DescriptorRegistrationTest {
    // Abstract classes with lists of descriptors
    private static final List<Class<? extends Describable>> DESCRIPTOR_ABSTRACT_CLASSES_TO_TEST = List.of(
            ModelConfiguration.class,
            PromptConfiguration.class,
            SkillConfiguration.class
    );

    // Classes that extend something from DESCRIPTOR_ABSTRACT_CLASSES_TO_TEST
    private static final List<Class<? extends Describable<?>>> CONCRETE_EXTENSION_IMPLEMENTATIONS_TO_TEST = List.of(
            GeminiModelConfiguration.class,
            OpenAIModelConfiguration.class,
            AnthropicModelConfiguration.class,
            OllamaModelConfiguration.class,
            SimplePrompt.class,
            FullPrompt.class,
            BasicSkillConfiguration.class,
            BasicSkillFileConfiguration.class,
            DeclaredSkillConfiguration.class,
            GithubFolderSkillConfiguration.class
    );

    // 3. Classes that extend Descriptor directly.
    private static final List<Class<? extends Describable<?>>> STANDALONE_CONCRETE_CLASSES_TO_TEST = List.of(
            AgenticCodeReviewSettings.class,
            GeminiClientConfiguration.class,
            AnthropicClientConfiguration.class,
            OllamaClientConfiguration.class,
            OpenAIClientConfiguration.class
    );

    @Test
    public void verifyAbstractDescriptorListsAreNotEmpty(JenkinsRule j) {
        for (var abstractClass : DESCRIPTOR_ABSTRACT_CLASSES_TO_TEST) {
            assertFalse(
                    j.jenkins.getDescriptorList(abstractClass).isEmpty(),
                    "Descriptor list for abstract class " + abstractClass.getName() + " should not be empty"
            );
        }
    }

    @Test
    public void verifyConcreteExtensionsAreRegisteredUnderAbstractBase(JenkinsRule j) {
        for (var concreteClass : CONCRETE_EXTENSION_IMPLEMENTATIONS_TO_TEST) {
            boolean exists = j.jenkins.getExtensionList(hudson.model.Descriptor.class).stream()
                    .anyMatch(d -> d.clazz.equals(concreteClass));
            assertTrue(
                    exists,
                    "Expected " + concreteClass.getName() + " to have a registered Descriptor in Jenkins"
            );
        }
    }

    @Test
    public void verifyStandaloneConcreteDescriptorsAreRegistered(JenkinsRule j) {
        for (var concreteClass : STANDALONE_CONCRETE_CLASSES_TO_TEST) {
            assertNotNull(
                    j.jenkins.getDescriptorOrDie(concreteClass),
                    "Missing standalone descriptor for " + concreteClass.getName()
            );
        }
    }
}