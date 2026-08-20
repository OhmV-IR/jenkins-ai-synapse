package io.ohmvir.plugins.jenkinscr.configuration.models;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.ohmvir.plugins.jenkinscr.api.models.ModelProviderType;
import io.ohmvir.plugins.jenkinscr.utils.SecretsUtils;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;

public class OllamaModelConfiguration extends ModelConfiguration {
    public String apiBaseUrlCredentialId;

    @DataBoundConstructor
    public OllamaModelConfiguration(String apiBaseUrlCredentialId, String modelName) throws Descriptor.FormException {
        super(modelName);
        if (SecretsUtils.getSecretText(apiBaseUrlCredentialId, null) == null) {
            throw new Descriptor.FormException("apiUrlCredentialId does not resolve to a valid string credential", "apiUrlCredentialId");
        }
        this.apiBaseUrlCredentialId = apiBaseUrlCredentialId;
    }

    @Override
    public ModelProviderType getProviderType() {
        return ModelProviderType.OLLAMA;
    }

    @Extension
    public static class DescriptorImpl extends ModelConfiguration.DescriptorImpl {
        private final static String MODELS_LIST_API_SUFFIX = "/api/tags";
        private transient final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        public DescriptorImpl() {
            super(OllamaModelConfiguration.class);
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Ollama Model";
        }

        public ListBoxModel doFillApiBaseUrlCredentialIdItems(
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

        public ListBoxModel doFillModelNameItems(@QueryParameter String apiBaseUrlCredentialId) {
            try {
                HttpRequest modelsListReq = HttpRequest.newBuilder()
                        .uri(URI.create(SecretsUtils.getSecretText(apiBaseUrlCredentialId, null) + MODELS_LIST_API_SUFFIX))
                        .build();
                HttpResponse<String> response = httpClient.send(modelsListReq, HttpResponse.BodyHandlers.ofString());
                JsonObject resJson = JsonParser.parseString(response.body()).getAsJsonObject();
                ListBoxModel models = new ListBoxModel();
                resJson.get("models")
                        .getAsJsonArray()
                        .forEach(model -> models.add(model.getAsJsonObject().get("name").getAsString(),
                                model.getAsJsonObject().get("model").getAsString())
                        );
                return models;
            } catch (Exception e) {
                return new ListBoxModel();
            }
        }

        @POST
        public FormValidation doCheckApiBaseUrlCredentialId(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("API Base URL is required");
            }
            if (SecretsUtils.getSecretText(value, null) == null) {
                return FormValidation.error("API Base URL does not resolve to a string credential");
            }
            return FormValidation.ok();
        }
    }
}
