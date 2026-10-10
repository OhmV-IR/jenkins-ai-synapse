package io.ohmvir.plugins.jenkinsaisynapse.api.response;

import io.ohmvir.plugins.jenkinsaisynapse.api.message.ContentPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.Message;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.Role;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelFinishReason;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * What a model produced in one call: its message, why it stopped and the tokens it used.
 *
 * @param message The model's message, with the {@link Role#ASSISTANT} role.
 * @param finishReason Why the model stopped. {@link ModelFinishReason#TOOL_CALLS} means it is waiting for the results
 *                     of the tools it called.
 * @param usage The tokens used, if the provider reports them.
 */
public record StepResult(
        Message message,
        ModelFinishReason finishReason,
        @Nullable TokenUsage usage) {
    public StepResult {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(finishReason, "finishReason");
        if (message.getRole() != Role.ASSISTANT) {
            throw new IllegalArgumentException("A step result must hold an ASSISTANT message");
        }
    }

    /**
     * Creates a result from the parts the model produced. When the provider does not report a finish reason, it is
     * {@link ModelFinishReason#TOOL_CALLS} if the model called tools and {@link ModelFinishReason#STOP} otherwise.
     */
    public static StepResult of(
            List<? extends ContentPart> parts, @Nullable ModelFinishReason finishReason, @Nullable TokenUsage usage) {
        Message message = new Message(Role.ASSISTANT, parts);
        if (finishReason == null) {
            finishReason = message.getToolCalls().isEmpty() ? ModelFinishReason.STOP : ModelFinishReason.TOOL_CALLS;
        }
        return new StepResult(message, finishReason, usage);
    }
}
