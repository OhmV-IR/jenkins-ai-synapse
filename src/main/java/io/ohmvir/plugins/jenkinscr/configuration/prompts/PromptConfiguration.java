package io.ohmvir.plugins.jenkinscr.configuration.prompts;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import io.ohmvir.plugins.jenkinscr.api.client.ModelRequest;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundSetter;

public abstract class PromptConfiguration implements Describable<PromptConfiguration>, ExtensionPoint {
    private @Getter String promptId = "";
    public abstract ModelRequest CreateRequest();

    public PromptConfiguration(String promptId){
        this.promptId = promptId;
    }

    @DataBoundSetter
    public void setPromptId(String promptId) {
        this.promptId = promptId;
    }
}
