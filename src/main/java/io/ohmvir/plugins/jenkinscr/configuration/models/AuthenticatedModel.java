package io.ohmvir.plugins.jenkinscr.configuration.models;

import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinscr.utils.SecretsUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public abstract class AuthenticatedModel extends Model {

    public AuthenticatedModel(String modelName, String apiKeyCredentialsId) throws Descriptor.FormException {
        super(modelName);
        if(SecretsUtils.getSecretText(apiKeyCredentialsId, null) == null){
            throw new Descriptor.FormException("API Key credentials id is invalid", "apiKeyCredentialsId");
        }
        this.apiKeyCredentialsId = apiKeyCredentialsId;
    }

    public String apiKeyCredentialsId;
}
