package io.ohmvir.plugins.jenkinsaisynapse.api.request;

import io.ohmvir.plugins.jenkinsaisynapse.api.message.ContentPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.MediaPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.Message;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.Role;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.TextPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolCallPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolResultPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Everything sent to a model: the conversation so far, the system prompt, generation settings, the tools the model
 * may call and the skills it has. Instances are immutable, so one request can be shared and run any number of times;
 * derive variations with {@link #toBuilder()}. Run a request with
 * {@link io.ohmvir.plugins.jenkinsaisynapse.api.agent.AgentLoop}.
 * <pre>
 * ModelRequest request = ModelRequest.builder()
 *         .systemPrompt("You are a code reviewer")
 *         .user("Review this change")
 *         .tool(ToolSpec.of(readFileTool))
 *         .params(GenerationParams.builder().temperature(0.2).build())
 *         .build();
 * </pre>
 */
public final class ModelRequest {
    private final List<Message> messages;
    private final @Nullable String systemPrompt;
    private final GenerationParams params;
    private final List<ToolSpec> tools;
    private final List<SkillData> skills;
    private final Set<ModelOutputType> requiredOutputs;

    private ModelRequest(Builder builder) {
        this.messages = List.copyOf(builder.messages);
        this.systemPrompt = builder.systemPrompt;
        this.params = builder.params;
        this.tools = List.copyOf(builder.tools);
        this.skills = List.copyOf(builder.skills);
        this.requiredOutputs =
                builder.requiredOutputs.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(builder.requiredOutputs));
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return A builder starting from this request's contents.
     */
    public Builder toBuilder() {
        Builder builder = new Builder()
                .systemPrompt(systemPrompt)
                .params(params)
                .messages(messages)
                .tools(tools)
                .skills(skills);
        requiredOutputs.forEach(builder::requireOutput);
        return builder;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public @Nullable String getSystemPrompt() {
        return systemPrompt;
    }

    public GenerationParams getParams() {
        return params;
    }

    public List<ToolSpec> getTools() {
        return tools;
    }

    public List<SkillData> getSkills() {
        return skills;
    }

    /**
     * @return The kinds of output the response must contain.
     */
    public Set<ModelOutputType> getRequiredOutputs() {
        return requiredOutputs;
    }

    public @Nullable ToolSpec getTool(String name) {
        return tools.stream()
                .filter(tool -> tool.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * @return The kinds of input a model must accept to read this request's messages.
     */
    public Set<ModelInputType> getInputTypes() {
        Set<ModelInputType> types = EnumSet.noneOf(ModelInputType.class);
        for (Message message : messages) {
            for (ContentPart part : message.getParts()) {
                if (part instanceof MediaPart media) {
                    types.add(media.getKind().getInputType());
                } else if (part instanceof TextPart || part instanceof ToolResultPart) {
                    types.add(ModelInputType.TEXT);
                }
            }
        }
        return types;
    }

    /**
     * @return The kinds of output a model must be able to produce to serve this request.
     */
    public Set<ModelOutputType> getOutputTypes() {
        return requiredOutputs;
    }

    /**
     * @return The capabilities a model needs to serve this request.
     */
    public Set<ModelCapability> getRequiredCapabilities() {
        Set<ModelCapability> capabilities = EnumSet.noneOf(ModelCapability.class);
        capabilities.addAll(params.getRequiredCapabilities());
        if (systemPrompt != null) {
            capabilities.add(ModelCapability.ADJUSTABLE_SYSTEM_PROMPT);
        }
        boolean usesTools = !tools.isEmpty()
                || messages.stream()
                        .flatMap(message -> message.getParts().stream())
                        .anyMatch(part -> part instanceof ToolCallPart || part instanceof ToolResultPart);
        if (usesTools) {
            capabilities.add(ModelCapability.TOOLS);
        }
        if (!skills.isEmpty()) {
            capabilities.add(ModelCapability.SKILLS);
        }
        return capabilities;
    }

    public static final class Builder {
        private final List<Message> messages = new ArrayList<>();
        private @Nullable String systemPrompt;
        private GenerationParams params = GenerationParams.DEFAULTS;
        private final List<ToolSpec> tools = new ArrayList<>();
        private final List<SkillData> skills = new ArrayList<>();
        private final Set<ModelOutputType> requiredOutputs = new LinkedHashSet<>();

        private Builder() {}

        public Builder systemPrompt(@Nullable String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder params(GenerationParams params) {
            this.params = Objects.requireNonNull(params, "params");
            return this;
        }

        public Builder message(Message message) {
            messages.add(Objects.requireNonNull(message, "message"));
            return this;
        }

        public Builder messages(List<Message> messages) {
            messages.forEach(this::message);
            return this;
        }

        /**
         * Adds a user message with the given text.
         */
        public Builder user(String text) {
            return message(Message.user(text));
        }

        /**
         * Adds the parts to the last message if it is a user message, or else adds a user message with them, so that
         * text and attachments added separately reach the model as one turn.
         */
        public Builder addUserParts(ContentPart... parts) {
            return addUserParts(Arrays.asList(parts));
        }

        public Builder addUserParts(List<? extends ContentPart> parts) {
            if (parts.isEmpty()) {
                return this;
            }
            int last = messages.size() - 1;
            if (last >= 0 && messages.get(last).getRole() == Role.USER) {
                messages.set(last, messages.get(last).withAddedParts(parts));
            } else {
                messages.add(new Message(Role.USER, parts));
            }
            return this;
        }

        /**
         * Inserts messages before the ones already added, for example the history of a conversation being continued.
         */
        public Builder prependMessages(List<Message> history) {
            messages.addAll(0, List.copyOf(history));
            return this;
        }

        public Builder tool(ToolSpec tool) {
            Objects.requireNonNull(tool, "tool");
            tools.removeIf(existing -> existing.getName().equals(tool.getName()));
            tools.add(tool);
            return this;
        }

        public Builder tools(List<ToolSpec> tools) {
            tools.forEach(this::tool);
            return this;
        }

        public Builder skill(SkillData skill) {
            skills.add(Objects.requireNonNull(skill, "skill"));
            return this;
        }

        public Builder skills(List<SkillData> skills) {
            skills.forEach(this::skill);
            return this;
        }

        /**
         * Requires the response to contain an output of the given type, for example an image.
         */
        public Builder requireOutput(ModelOutputType outputType) {
            requiredOutputs.add(Objects.requireNonNull(outputType, "outputType"));
            return this;
        }

        /**
         * @throws IllegalStateException If the request has no messages.
         */
        public ModelRequest build() {
            if (messages.isEmpty()) {
                throw new IllegalStateException("A request needs at least one message");
            }
            return new ModelRequest(this);
        }
    }
}
