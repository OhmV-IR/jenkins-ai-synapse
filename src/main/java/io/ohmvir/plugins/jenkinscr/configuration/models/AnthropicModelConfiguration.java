package io.ohmvir.plugins.jenkinscr.configuration.models;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.models.ModelListPage;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.ohmvir.plugins.jenkinscr.api.models.ModelProviderType;
import io.ohmvir.plugins.jenkinscr.utils.SecretsUtils;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class AnthropicModelConfiguration extends AuthenticatedModelConfiguration {

    @DataBoundConstructor
    public AnthropicModelConfiguration(String modelName, String apiKeyCredentialsId) throws Descriptor.FormException {
        super(modelName, apiKeyCredentialsId);
    }

    @Override
    public ModelProviderType getProviderType() {
        return ModelProviderType.ANTHROPIC;
    }

    @Extension
    public static class DescriptorImpl extends AuthenticatedModelConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Anthropic Model";
        }

        @Override
        public ListBoxModel doFillModelNameItems(@QueryParameter String apiKeyCredentialsId) {
            if (doCheckApiKeyCredentialsId(apiKeyCredentialsId).kind != FormValidation.Kind.OK) {
                return new StandardListBoxModel();
            }
            try {
                AnthropicClient client = AnthropicOkHttpClient.builder()
                        .apiKey(SecretsUtils.getSecretText(apiKeyCredentialsId, null))
                        .build();
                ModelListPage models = client.models().list();
                ListBoxModel modelsMap = new ListBoxModel();
                models.data().stream()
                        .filter(model -> {
                            if (model.capabilities().isEmpty()) {
                                return false;
                            }
                            return model.capabilities().get().structuredOutputs().supported();
                        })
                        .forEach(model -> modelsMap.add(model.displayName(), model.id()));
                return modelsMap;
            } catch (Exception e) {
                return new StandardListBoxModel();
            }
        }
    }
}
