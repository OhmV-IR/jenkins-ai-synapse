package io.ohmvir.plugins.jenkinscr.configuration.models;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.google.genai.Client;
import com.google.genai.Pager;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.ListModelsConfig;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.ohmvir.plugins.jenkinscr.api.ModelProviderType;
import io.ohmvir.plugins.jenkinscr.utils.SecretsUtils;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.util.Collections;

public class GeminiModelConfiguration extends AuthenticatedModelConfiguration {

    @DataBoundConstructor
    public GeminiModelConfiguration(String modelName, String apiKeyCredentialsId) throws Descriptor.FormException {
        super(modelName, apiKeyCredentialsId);
    }

    @Override
    public ModelProviderType getProviderType() {
        return ModelProviderType.GEMINI;
    }

    @Extension
    public static class DescriptorImpl extends AuthenticatedModelConfiguration.DescriptorImpl {

        @Override
        public @NonNull String getDisplayName() {
            return "Gemini Model";
        }

        @Override
        public ListBoxModel doFillModelNameItems(@QueryParameter String apiKeyCredentialsId) {
            if(doCheckApiKeyCredentialsId(apiKeyCredentialsId).kind != FormValidation.Kind.OK){
                return new StandardListBoxModel();
            }
            try {
                Client client = Client.builder()
                        .apiKey(SecretsUtils.getSecretText(apiKeyCredentialsId, null))
                        .httpOptions(HttpOptions.builder()
                                .apiVersion("v1")
                                .retryOptions(
                                        HttpRetryOptions.builder()
                                                .attempts(3)
                                                .httpStatusCodes(408, 429)
                                                .build()
                                )
                                .build())
                        .build();
                Pager<com.google.genai.types.Model> pager = client.models.list(ListModelsConfig.builder().build());
                ListBoxModel modelsMap = new ListBoxModel();
                for (com.google.genai.types.Model model : pager) {
                    if (model.displayName().isEmpty() || model.name().isEmpty()) {
                        continue;
                    }
                    modelsMap.add(model.displayName().get(), model.name().get());
                }
                client.close();
                return modelsMap;
            } catch(Exception e){
                return new StandardListBoxModel();
            }
        }
    }
}
