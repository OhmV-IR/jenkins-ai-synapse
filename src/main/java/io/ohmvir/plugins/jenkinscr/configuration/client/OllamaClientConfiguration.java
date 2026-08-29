package io.ohmvir.plugins.jenkinscr.configuration.client;

import hudson.Extension;
import hudson.model.Descriptor;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class OllamaClientConfiguration extends ModelClientConfiguration {
    private @Getter Long keepAliveSeconds = 0L;

    @DataBoundSetter
    public void setKeepAliveTime(Long newKeepAliveSeconds){
        this.keepAliveSeconds = newKeepAliveSeconds;
    }

    @DataBoundConstructor
    public OllamaClientConfiguration(Long timeoutSeconds, Long keepAliveSeconds) throws Descriptor.FormException {
        super(timeoutSeconds);
        this.keepAliveSeconds = keepAliveSeconds;
    }

    @Extension
    public static class DescriptorImpl extends ModelClientConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Ollama Client Configuration";
        }
    }
}
