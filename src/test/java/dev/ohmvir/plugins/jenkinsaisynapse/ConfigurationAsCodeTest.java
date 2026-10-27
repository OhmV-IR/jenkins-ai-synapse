package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.model.Descriptor;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.PromptsConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.RequestsConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.SkillsConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.SimplePrompt;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.BasicSkillConfiguration;
import org.jenkinsci.Symbol;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.DataBoundConstructor;

@WithJenkinsConfiguredWithCode
public class ConfigurationAsCodeTest {

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void configuresModelsPromptsAndSkills(JenkinsConfiguredWithCodeRule j) {
        ModelConfiguration model = ModelsConfiguration.get().getModels().get(0);
        assertEquals("casc:large", model.getModelId());
        assertEquals("Large model", model.getModelDisplayName());

        SimplePrompt prompt = (SimplePrompt) PromptsConfiguration.get().getPrompt("summarize");
        assertEquals("Summarize the build", prompt.getPromptText());

        BasicSkillConfiguration skill =
                (BasicSkillConfiguration) SkillsConfiguration.get().getSkills().get(0);
        assertEquals("reviewer", skill.skillName);

    }

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void configuresTheDefaultMaximumSteps(JenkinsConfiguredWithCodeRule j) {
        assertEquals(40, RequestsConfiguration.get().getMaxSteps());
        assertEquals(40, ModelRequest.getDefaultMaxSteps());

        ModelRequest request = new ModelRequest();
        assertEquals(40, request.getMaxSteps());
        assertEquals(40, new ModelRequest(request).getMaxSteps());

        request.setMaxSteps(5);
        assertEquals(5, request.getMaxSteps());
        assertEquals(5, new ModelRequest(request).getMaxSteps());
    }

    public static class CascModelConfiguration extends ModelConfiguration {
        @DataBoundConstructor
        public CascModelConfiguration(String modelName, String modelDisplayName) throws Descriptor.FormException {
            super(modelName, modelDisplayName);
        }

        @Override
        public String getProviderType() {
            return "casc";
        }

        @TestExtension
        @Symbol("cascModel")
        public static class DescriptorImpl extends ModelConfiguration.DescriptorImpl {}
    }
}
