package io.ohmvir.plugins.jenkinscr;

import hudson.model.Describable;
import io.ohmvir.plugins.jenkinscr.configuration.AgenticCodeReviewSettings;
import io.ohmvir.plugins.jenkinscr.configuration.agents.AgentConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.models.*;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@WithJenkins
public class DescriptorRegistrationTest {
    private static final List<Class<? extends Describable<?>>> DESCRIPTOR_CLASSES_TO_TEST = List.of(
            AgentConfiguration.class,
            AnthropicModelConfiguration.class,
            GeminiModelConfiguration.class,
            OllamaModelConfiguration.class,
            OpenAIModelConfiguration.class,
            AgenticCodeReviewSettings.class
    );
    @Test
    public void verifyDescriptorsAreRegistered(JenkinsRule j){
        for(Class<? extends Describable<?>> c : DESCRIPTOR_CLASSES_TO_TEST){
            assertNotNull(j.jenkins.getDescriptorOrDie(c));
        }
    }
}
