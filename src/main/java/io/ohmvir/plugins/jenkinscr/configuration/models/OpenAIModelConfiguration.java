package io.ohmvir.plugins.jenkinscr.configuration.models;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.models.ModelListPage;
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
import java.util.List;

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

        public ListBoxModel doFillModelNameItems(@QueryParameter String apiKeyCredentialsId){
            if(doCheckApiKeyCredentialsId(apiKeyCredentialsId).kind != FormValidation.Kind.OK){
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
            } catch(Exception e){
                return new StandardListBoxModel();
            }
        }
    }
}
