package io.ohmvir.plugins.jenkinscr.forms;

import hudson.util.FormValidation;
import io.ohmvir.plugins.jenkinscr.configuration.agents.AgentConfiguration;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithJenkins
public class AgentConfigurationValidationTests {
    @Test
    public void checkAgentConfigurationFormValidation(JenkinsRule j) {
        AgentConfiguration.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(AgentConfiguration.DescriptorImpl.class);

        FormValidation nullProvider = descriptor.doCheckProvider(null);
        assertEquals(FormValidation.Kind.ERROR, nullProvider.kind);

        FormValidation invalidMaxOutputTokens = descriptor.doCheckMaxOutputTokensPerPrompt((long) -5000);
        assertEquals(FormValidation.Kind.ERROR, invalidMaxOutputTokens.kind);

        FormValidation noTemp = descriptor.doCheckTemperature(null);
        assertEquals(FormValidation.Kind.ERROR, noTemp.kind);
        FormValidation lowTemp = descriptor.doCheckTemperature(-1.0);
        assertEquals(FormValidation.Kind.ERROR, lowTemp.kind);
        FormValidation highTemp = descriptor.doCheckTemperature(2.0);
        assertEquals(FormValidation.Kind.ERROR, highTemp.kind);
    }
}
