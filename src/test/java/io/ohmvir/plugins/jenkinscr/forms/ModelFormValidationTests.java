package io.ohmvir.plugins.jenkinscr.forms;

import hudson.util.FormValidation;
import io.ohmvir.plugins.jenkinscr.configuration.agents.AgentConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.models.AnthropicModel;
import io.ohmvir.plugins.jenkinscr.configuration.models.GeminiModel;
import io.ohmvir.plugins.jenkinscr.configuration.models.OllamaModel;
import io.ohmvir.plugins.jenkinscr.configuration.models.OpenAIModel;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithJenkins
public class ModelFormValidationTests {
    @Test
    public void checkOllamaFormValidation(JenkinsRule j){
        OllamaModel.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(OllamaModel.DescriptorImpl.class);

        FormValidation emptyApiUrl = descriptor.doCheckApiBaseUrlCredentialId("");
        assertEquals(FormValidation.Kind.ERROR, emptyApiUrl.kind);
        FormValidation invalidCredentialUrl = descriptor.doCheckApiBaseUrlCredentialId("not_a_real_credential_id");
        assertEquals(FormValidation.Kind.ERROR, invalidCredentialUrl.kind);

        FormValidation invalidModelName = descriptor.doCheckModelName(" ");
        assertEquals(FormValidation.Kind.ERROR, invalidModelName.kind);
        FormValidation validModelName = descriptor.doCheckModelName("model_name");
        assertEquals(FormValidation.Kind.OK, validModelName.kind);
    }

    @Test
    public void checkAnthropicFormValidation(JenkinsRule j){
        AnthropicModel.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(AnthropicModel.DescriptorImpl.class);

        FormValidation emptyApiKey = descriptor.doCheckApiKeyCredentialsId("");
        assertEquals(FormValidation.Kind.ERROR, emptyApiKey.kind);
        FormValidation invalidCredentialUrl = descriptor.doCheckApiKeyCredentialsId("not_a_real_credential_id");
        assertEquals(FormValidation.Kind.ERROR, invalidCredentialUrl.kind);

        FormValidation invalidModelName = descriptor.doCheckModelName("");
        assertEquals(FormValidation.Kind.ERROR, invalidModelName.kind);

        FormValidation validModelName = descriptor.doCheckModelName("model_name");
        assertEquals(FormValidation.Kind.OK, validModelName.kind);
    }

    @Test
    public void checkGeminiFormValidation(JenkinsRule j){
        GeminiModel.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(GeminiModel.DescriptorImpl.class);

        FormValidation emptyApiKey = descriptor.doCheckApiKeyCredentialsId("");
        assertEquals(FormValidation.Kind.ERROR, emptyApiKey.kind);
        FormValidation invalidCredentialUrl = descriptor.doCheckApiKeyCredentialsId("not_a_real_credential_id");
        assertEquals(FormValidation.Kind.ERROR, invalidCredentialUrl.kind);

        FormValidation invalidModelName = descriptor.doCheckModelName("");
        assertEquals(FormValidation.Kind.ERROR, invalidModelName.kind);

        FormValidation validModelName = descriptor.doCheckModelName("model_name");
        assertEquals(FormValidation.Kind.OK, validModelName.kind);
    }

    @Test
    public void checkOpenAIFormValidation(JenkinsRule j){
        OpenAIModel.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(OpenAIModel.DescriptorImpl.class);

        FormValidation emptyApiKey = descriptor.doCheckApiKeyCredentialsId("");
        assertEquals(FormValidation.Kind.ERROR, emptyApiKey.kind);
        FormValidation invalidCredentialUrl = descriptor.doCheckApiKeyCredentialsId("not_a_real_credential_id");
        assertEquals(FormValidation.Kind.ERROR, invalidCredentialUrl.kind);

        FormValidation invalidModelName = descriptor.doCheckModelName("");
        assertEquals(FormValidation.Kind.ERROR, invalidModelName.kind);

        FormValidation validModelName = descriptor.doCheckModelName("model_name");
        assertEquals(FormValidation.Kind.OK, validModelName.kind);
    }
}
