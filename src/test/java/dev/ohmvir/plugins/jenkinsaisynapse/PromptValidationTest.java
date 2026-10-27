package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.MaxOutputTokensContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TemperatureContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.PromptsConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.FullPrompt;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.SimplePrompt;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class PromptValidationTest {

    @Test
    public void promptIdCheckRejectsMissingAndBlankValues(JenkinsRule j) {
        FullPrompt.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(FullPrompt.DescriptorImpl.class);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckPromptId(null).kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckPromptId("  ").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckPromptId("unique").kind);
    }

    @Test
    public void promptIdCheckAcceptsASavedPromptsOwnId(JenkinsRule j) throws Exception {
        PromptsConfiguration.get().setPrompts(List.of(new SimplePrompt("a", "first")));
        FullPrompt.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(FullPrompt.DescriptorImpl.class);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckPromptId("a").kind);
    }

    @Test
    public void addingAPromptWithAnExistingIdIsRejectedOnSave(JenkinsRule j) throws Exception {
        PromptsConfiguration config = PromptsConfiguration.get();
        config.setPrompts(List.of(new SimplePrompt("a", "first")));
        assertThrows(
                IllegalArgumentException.class,
                () -> config.setPrompts(List.of(new SimplePrompt("a", "first"), newFullPrompt("a", 0, 0))));
        assertEquals(1, config.getPrompts().size());
    }

    @Test
    public void unsetMaxOutputTokensIsNotSentInTheRequest(JenkinsRule j) throws Exception {
        ModelRequest request = newFullPrompt("full", 0, 0).createRequest();
        assertTrue(inputsOfType(request, MaxOutputTokensContent.class).isEmpty());
    }

    @Test
    public void maxOutputTokensIsSentWhenSet(JenkinsRule j) throws Exception {
        ModelRequest request = newFullPrompt("full", 0, 256).createRequest();
        List<MaxOutputTokensContent> inputs = inputsOfType(request, MaxOutputTokensContent.class);
        assertEquals(1, inputs.size());
        assertEquals(256, inputs.get(0).getMaxOutputTokens());
    }

    @Test
    public void temperatureZeroIsStillSent(JenkinsRule j) throws Exception {
        ModelRequest request = newFullPrompt("full", 0, 0).createRequest();
        assertEquals(1, inputsOfType(request, TemperatureContent.class).size());
    }

    @Test
    public void negativeMaxOutputTokensIsRejected(JenkinsRule j) {
        assertThrows(Descriptor.FormException.class, () -> newFullPrompt("full", 0, -1));
        FullPrompt.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(FullPrompt.DescriptorImpl.class);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckMaxOutputTokensCount(-1).kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckMaxOutputTokensCount(0).kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckMaxOutputTokensCount(100).kind);
    }

    private static FullPrompt newFullPrompt(String id, double temperature, long maxOutputTokens)
            throws Descriptor.FormException {
        return new FullPrompt(id, "text", "system", temperature, maxOutputTokens, ModelThinkingLevel.OFF);
    }

    private static <T extends ModelInput> List<T> inputsOfType(ModelRequest request, Class<T> type) {
        return request.getModelInputs().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }
}
