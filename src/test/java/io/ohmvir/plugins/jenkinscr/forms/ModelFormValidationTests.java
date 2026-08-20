package io.ohmvir.plugins.jenkinscr.forms;

import hudson.util.FormValidation;
import io.ohmvir.plugins.jenkinscr.configuration.models.AnthropicModelConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.models.GeminiModelConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.models.OllamaModelConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.models.OpenAIModelConfiguration;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithJenkins
public class ModelFormValidationTests {
    @Test
    public void checkOllamaFormValidation(JenkinsRule j) {
        OllamaModelConfiguration.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(OllamaModelConfiguration.DescriptorImpl.class);

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
    public void checkAnthropicFormValidation(JenkinsRule j) {
        AnthropicModelConfiguration.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(AnthropicModelConfiguration.DescriptorImpl.class);

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
    public void checkGeminiFormValidation(JenkinsRule j) {
        GeminiModelConfiguration.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(GeminiModelConfiguration.DescriptorImpl.class);

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
    public void checkOpenAIFormValidation(JenkinsRule j) {
        OpenAIModelConfiguration.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(OpenAIModelConfiguration.DescriptorImpl.class);

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
