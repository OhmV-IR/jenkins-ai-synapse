package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * Prior conversation history attached as input to a model. History is not stored by this plugin, it is taken from
 * {@link ModelResponse#getConversation()} of a previous request.
 */
public class InputConversationContent extends ModelInput {
    private final List<ModelContent> history;

    public InputConversationContent(List<ModelContent> history) {
        this.history = new ArrayList<>(history);
    }

    public InputConversationContent(ModelResponse previousResponse) {
        this(previousResponse.getConversation());
    }

    public List<ModelContent> getHistory() {
        return Collections.unmodifiableList(history);
    }

    @Extension
    public static class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.CONVERSATIONS);
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of();
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Input Conversation Content";
        }
    }
}
