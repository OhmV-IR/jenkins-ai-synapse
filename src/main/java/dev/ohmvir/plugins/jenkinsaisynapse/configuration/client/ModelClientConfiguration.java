package dev.ohmvir.plugins.jenkinsaisynapse.configuration.client;

import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 * Global settings shared by every model of one client (provider). Subclasses are {@link GlobalConfiguration}s, so
 * they are their own descriptor: annotate them with {@code @Extension} and {@code @Symbol}, give them a no-argument
 * constructor that calls {@link #load()}, and expose settings through {@link DataBoundSetter}s that call
 * {@link #save()}. This makes them editable on the System configuration page and settable with configuration as code.
 */
public abstract class ModelClientConfiguration extends GlobalConfiguration {
    public static final long DEFAULT_TIMEOUT_SECONDS = 300;

    /**
     * Maximum time a whole request, including all tool call round trips, may take. 0 means no timeout.
     */
    private @Getter long timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

    protected ModelClientConfiguration() {}

    @DataBoundSetter
    public void setTimeoutSeconds(long timeoutSeconds) {
        if (timeoutSeconds < 0) {
            throw new IllegalArgumentException("Timeout seconds must not be negative");
        }
        this.timeoutSeconds = timeoutSeconds;
        save();
    }

    @POST
    public FormValidation doCheckTimeoutSeconds(@QueryParameter String value) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        if (value == null || value.isBlank()) {
            return FormValidation.error("Timeout seconds is required, use 0 for no timeout");
        }
        try {
            if (Long.parseLong(value.trim()) < 0) {
                return FormValidation.error("Timeout seconds must not be negative");
            }
        } catch (NumberFormatException e) {
            return FormValidation.error("Timeout seconds must be a whole number");
        }
        return FormValidation.ok();
    }
}
