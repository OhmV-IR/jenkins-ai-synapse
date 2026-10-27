package dev.ohmvir.plugins.jenkinsaisynapse.configuration;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 * Settings that apply to every AI request, whichever model runs it. Edited on the System configuration page under
 * Manage Jenkins, and can be set with configuration as code under {@code unclassified.aiRequests}.
 */
@Extension
@Symbol("aiRequests")
public class RequestsConfiguration extends GlobalConfiguration {
    /** The value of {@link #getMaxSteps()} until it is configured. */
    public static final int DEFAULT_MAX_STEPS = 25;

    private int maxSteps = DEFAULT_MAX_STEPS;

    public RequestsConfiguration() {
        load();
    }

    public static RequestsConfiguration get() {
        return ExtensionList.lookupSingleton(RequestsConfiguration.class);
    }

    /**
     * The maximum number of times the model is called for a request that does not set its own limit. Each time the
     * model calls tools, the results are sent back to it in another step. A stored value that is not positive, which
     * a hand-edited configuration file could contain, reads as {@link #DEFAULT_MAX_STEPS}.
     */
    public int getMaxSteps() {
        return maxSteps > 0 ? maxSteps : DEFAULT_MAX_STEPS;
    }

    /**
     * @throws IllegalArgumentException if {@code maxSteps} is not greater than 0
     */
    @DataBoundSetter
    public void setMaxSteps(int maxSteps) {
        if (maxSteps <= 0) {
            throw new IllegalArgumentException("Maximum steps must be greater than 0");
        }
        this.maxSteps = maxSteps;
        save();
    }

    @POST
    public FormValidation doCheckMaxSteps(@QueryParameter String value) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        if (value == null || value.isBlank()) {
            return FormValidation.error("Maximum steps is required");
        }
        try {
            if (Integer.parseInt(value.trim()) <= 0) {
                return FormValidation.error("Maximum steps must be greater than 0");
            }
        } catch (NumberFormatException e) {
            return FormValidation.error("Maximum steps must be a whole number");
        }
        return FormValidation.ok();
    }

    @Override
    public @NonNull String getDisplayName() {
        return "AI Requests";
    }
}
