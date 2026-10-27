package dev.ohmvir.plugins.jenkinsaisynapse.api;

import hudson.model.Run;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jenkins.model.RunAction2;
import org.jspecify.annotations.Nullable;

/**
 * Keeps the conversations of a build, so they are visible from the build page and are deleted together with the
 * build.
 */
public class ModelConversationsAction implements RunAction2 {
    private static final Object LOCK = new Object();

    private final List<ModelConversation> conversations = new ArrayList<>();
    private transient Run<?, ?> run;

    /**
     * Adds a conversation to the build and saves the build.
     */
    public static void record(Run<?, ?> run, ModelConversation conversation) throws IOException {
        ModelConversationsAction action;
        synchronized (LOCK) {
            action = run.getAction(ModelConversationsAction.class);
            if (action == null) {
                action = new ModelConversationsAction();
                run.addAction(action);
            }
        }
        action.add(conversation);
        run.save();
    }

    private synchronized void add(ModelConversation conversation) {
        conversations.add(conversation);
    }

    public synchronized List<ModelConversation> getConversations() {
        return Collections.unmodifiableList(new ArrayList<>(conversations));
    }

    public @Nullable Run<?, ?> getRun() {
        return run;
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        this.run = r;
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        this.run = r;
    }

    @Override
    public String getIconFileName() {
        return "symbol-chatbubbles-outline plugin-ionicons-api";
    }

    @Override
    public String getDisplayName() {
        return "AI Conversations";
    }

    @Override
    public String getUrlName() {
        return "ai-conversations";
    }
}
