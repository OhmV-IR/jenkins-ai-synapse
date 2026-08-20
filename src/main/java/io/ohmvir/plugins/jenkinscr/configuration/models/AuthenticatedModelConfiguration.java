package io.ohmvir.plugins.jenkinscr.configuration.models;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.ohmvir.plugins.jenkinscr.utils.SecretsUtils;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.util.Collections;

public abstract class AuthenticatedModelConfiguration extends ModelConfiguration {

    public AuthenticatedModelConfiguration(String modelName, String apiKeyCredentialsId) throws Descriptor.FormException {
        super(modelName);
        if(SecretsUtils.getSecretText(apiKeyCredentialsId, null) == null){
            throw new Descriptor.FormException("API Key credentials id is invalid", "apiKeyCredentialsId");
        }
        this.apiKeyCredentialsId = apiKeyCredentialsId;
    }

    public String apiKeyCredentialsId;

    public abstract static class DescriptorImpl extends ModelConfiguration.DescriptorImpl {

        public DescriptorImpl(Class<? extends ModelConfiguration> clazz){
            super(clazz);
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

        public abstract ListBoxModel doFillModelNameItems(@QueryParameter String apiKeyCredentialsId);

    }
}
