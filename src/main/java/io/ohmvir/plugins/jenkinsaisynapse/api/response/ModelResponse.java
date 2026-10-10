package io.ohmvir.plugins.jenkinsaisynapse.api.response;

import io.ohmvir.plugins.jenkinsaisynapse.api.message.ContentPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.MediaPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.Message;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.Role;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ThinkingPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolCallPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolResultPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelFinishReason;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * The result of running a request: the whole transcript, the messages produced while running it, why the model
 * stopped and the tokens it used. Instances are immutable.
 */
public final class ModelResponse {
    private final String modelId;
    private final List<Message> transcript;
    private final int newMessagesStart;
    private final ModelFinishReason finishReason;
    private final @Nullable TokenUsage usage;
    private final int steps;

    /**
     * @param modelId The model that answered.
     * @param transcript The request's messages followed by the messages produced while running it.
     * @param newMessagesStart The index in the transcript of the first produced message.
     * @param finishReason Why the model stopped in its last step.
     * @param usage The tokens used over all steps, or null if the provider did not report them.
     * @param steps The number of calls made to the model.
     */
    public ModelResponse(
            String modelId,
            List<Message> transcript,
            int newMessagesStart,
            ModelFinishReason finishReason,
            @Nullable TokenUsage usage,
            int steps) {
        if (newMessagesStart < 0 || newMessagesStart > transcript.size()) {
            throw new IllegalArgumentException("newMessagesStart is outside the transcript");
        }
        this.modelId = modelId;
        this.transcript = List.copyOf(transcript);
        this.newMessagesStart = newMessagesStart;
        this.finishReason = finishReason;
        this.usage = usage;
        this.steps = steps;
    }

    public String getModelId() {
        return modelId;
    }

    /**
     * @return The request's messages followed by every message produced while running it. Pass it as the messages of
     * a new request to continue the conversation.
     */
    public List<Message> getTranscript() {
        return transcript;
    }

    /**
     * @return The assistant and tool messages produced while running the request.
     */
    public List<Message> getNewMessages() {
        return transcript.subList(newMessagesStart, transcript.size());
    }

    public ModelFinishReason getFinishReason() {
        return finishReason;
    }

    public @Nullable TokenUsage getUsage() {
        return usage;
    }

    public int getSteps() {
        return steps;
    }

    /**
     * @return The model's last message, which holds its final answer.
     */
    public @Nullable Message getFinalMessage() {
        List<Message> assistantMessages = getAssistantMessages();
        return assistantMessages.isEmpty() ? null : assistantMessages.get(assistantMessages.size() - 1);
    }

    /**
     * @return The text of the model's final answer.
     */
    public String getText() {
        Message finalMessage = getFinalMessage();
        return finalMessage == null ? "" : finalMessage.getText();
    }

    /**
     * @return All the text the model produced while running the request, including text written before tool calls.
     */
    public String getAllText() {
        return getAssistantMessages().stream().map(Message::getText).collect(Collectors.joining());
    }

    /**
     * @return All the thinking the model produced, separated by blank lines.
     */
    public String getThinking() {
        return getParts(ThinkingPart.class).stream()
                .map(ThinkingPart::getThinking)
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * @return Every part of the given type in the produced messages, in order.
     */
    public <T extends ContentPart> List<T> getParts(Class<T> type) {
        return getNewMessages().stream()
                .flatMap(message -> message.getParts(type).stream())
                .toList();
    }

    public List<ToolCallPart> getToolCalls() {
        return getParts(ToolCallPart.class);
    }

    public List<ToolResultPart> getToolResults() {
        return getParts(ToolResultPart.class);
    }

    /**
     * @return The images, audio, video and files the model produced.
     */
    public List<MediaPart> getMedia() {
        return getAssistantMessages().stream()
                .flatMap(message -> message.getParts(MediaPart.class).stream())
                .toList();
    }

    /**
     * @return The kinds of output the model produced.
     */
    public Set<ModelOutputType> getOutputTypes() {
        Set<ModelOutputType> types = EnumSet.noneOf(ModelOutputType.class);
        if (!getAllText().isEmpty()) {
            types.add(ModelOutputType.UNSTRUCTURED_TEXT);
        }
        getMedia().forEach(media -> types.add(media.getKind().getOutputType()));
        return types;
    }

    private List<Message> getAssistantMessages() {
        return getNewMessages().stream()
                .filter(message -> message.getRole() == Role.ASSISTANT)
                .toList();
    }
}
