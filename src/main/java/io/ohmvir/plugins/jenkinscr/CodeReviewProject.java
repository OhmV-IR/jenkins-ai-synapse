package io.ohmvir.plugins.jenkinscr;

import hudson.Extension;
import hudson.model.*;
import hudson.security.Permission;
import jakarta.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.verb.POST;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class CodeReviewProject extends AbstractItem implements TopLevelItem {
    protected CodeReviewProject(ItemGroup parent, String name) {
        super(parent, name);
    }

    @Override
    public Collection<? extends Job> getAllJobs() {
        return List.of();
    }

    @Override
    public TopLevelItemDescriptor getDescriptor() {
        return (DescriptorImpl) Jenkins.get().getDescriptorOrDie(getClass());
    }

    @RequirePOST
    @POST
    public synchronized void doConfigSubmit(StaplerRequest2 req, StaplerResponse2 res) throws IOException, ServletException {
        checkPermission(Permission.CONFIGURE);

        JSONObject config = req.getSubmittedForm();
        if (config.has("displayName")) {
            setDisplayName(config.getString("displayName"));
        }
        save();
        res.sendRedirect2(".");
    }

    @Extension
    public static class DescriptorImpl extends TopLevelItemDescriptor {
        @Override
        public @NonNull String getDisplayName() {
            return "AI Code Review Project";
        }

        @Override
        public TopLevelItem newInstance(ItemGroup parent, String name) {
            return new CodeReviewProject(parent, name);
        }

        @Override
        public @NonNull String getDescription() {
            return "A project in which agents can review code with managed context and use jenkins agents to call tools";
        }
    }
}
