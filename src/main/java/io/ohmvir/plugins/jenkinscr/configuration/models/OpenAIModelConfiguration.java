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
    public static class DescriptorImpl extends Descriptor<ModelConfiguration> {

        @Override
        public @NonNull String getDisplayName() {
            return "OpenAI Model";
        }

        public ListBoxModel doFillApiKeyCredentialsIdItems (
                @AncestorInPath Item context,
                @QueryParameter String apiBaseUrlCredentialId) {

            if (context == null ? !Jenkins.get().hasPermission(Jenkins.ADMINISTER) : !context.hasPermission(Item.CONFIGURE)) {
                return new StandardListBoxModel().includeCurrentValue(apiBaseUrlCredentialId);
            }

            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(
                            ACL.SYSTEM2,
                            context,
                            StandardCredentials.class,
                            Collections.emptyList(),
                            CredentialsMatchers.instanceOf(StringCredentials.class)
                    );
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
                models.data().stream()
                        .filter(model -> !(model.id().contains("embedding") || model.id().contains("whisper")
                                || model.id().contains("tts") || model.id().contains("dall-e") || model.id().contains("babbage")
                                || model.id().contains("davinci")))
                        .filter(model -> modelSupportsCustomTools(model.id()))
                        .forEach(model -> modelsMap.add(model.id(), model.id()));
                return modelsMap;
            } catch(Exception e){
                return new StandardListBoxModel();
            }
        }

        private static final List<String> TOOL_SUPPORTED_PREFIXES = List.of(
                "gpt-4o",
                "gpt-4-turbo",
                "gpt-4",
                "o1",
                "o3-mini",
                "gpt-3.5-turbo"
        );

        private static boolean modelSupportsCustomTools(String modelId){
            return TOOL_SUPPORTED_PREFIXES.stream().anyMatch(modelId::startsWith);
        }

        @POST
        public FormValidation doCheckModelName(@QueryParameter String value){
            if(value == null || value.trim().isEmpty()){
                return FormValidation.error("Model name is required");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckApiKeyCredentialsId(@QueryParameter String value){
            if(value == null || value.trim().isEmpty()){
                return FormValidation.error("API key credentials id is required");
            }
            if(SecretsUtils.getSecretText(value, null) == null){
                return FormValidation.error("API key credentials id is required");
            }
            return FormValidation.ok();
        }
    }
}
