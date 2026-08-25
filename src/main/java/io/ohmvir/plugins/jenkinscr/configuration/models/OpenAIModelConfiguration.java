package io.ohmvir.plugins.jenkinscr.configuration.models;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.models.ModelListPage;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.ohmvir.plugins.jenkinscr.api.models.ModelProviderType;
import io.ohmvir.plugins.jenkinscr.utils.SecretsUtils;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class OpenAIModelConfiguration extends AuthenticatedModelConfiguration {

    @DataBoundConstructor
    public OpenAIModelConfiguration(String modelName, String apiKeyCredentialsId) throws Descriptor.FormException {
        super(modelName, apiKeyCredentialsId);
    }

    @Override
    public ModelProviderType getProviderType() {
        return ModelProviderType.OPENAI;
    }

    @Extension
    public static class DescriptorImpl extends AuthenticatedModelConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "OpenAI Model";
        }

        public ListBoxModel doFillModelNameItems(@QueryParameter String apiKeyCredentialsId) {
            if (doCheckApiKeyCredentialsId(apiKeyCredentialsId).kind != FormValidation.Kind.OK) {
                return new StandardListBoxModel();
            }
            try {
                OpenAIClient client = OpenAIOkHttpClient.builder()
                        .apiKey(SecretsUtils.getSecretText(apiKeyCredentialsId, null))
                        .followRedirects(true)
                        .build();
                ModelListPage models = client.models().list();
                ListBoxModel modelsMap = new ListBoxModel();
                models.data()
                        .forEach(model -> modelsMap.add(model.id(), model.id()));
                return modelsMap;
            } catch (Exception e) {
                return new StandardListBoxModel();
            }
        }
    }
}
