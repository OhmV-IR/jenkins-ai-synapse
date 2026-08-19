package io.ohmvir.plugins.jenkinscr.configuration.models;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.models.ModelListPage;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
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
        public ListBoxModel doFillModelNameItems(@QueryParameter String apiKeyCredentialsId){
            if(doCheckApiKeyCredentialsId(apiKeyCredentialsId).kind != FormValidation.Kind.OK){
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
            } catch(Exception e){
                return new StandardListBoxModel();
            }
        }
    }
}
