package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelFinishReason;
import java.util.*;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

public class ModelResponse {
    private @Getter final List<ModelOutput> outputs = new ArrayList<>();

    public ModelResponse(List<ModelOutput> outputs) {
        this.outputs.addAll(outputs);
    }

    /**
     * @return Every output of the given type, in the order the model produced them.
     */
    public <T extends ModelOutput> List<T> getOutputs(Class<T> type) {
        return outputs.stream().filter(type::isInstance).map(type::cast).toList();
    }

    /**
     * @return The model's final answer: the text it produced after its last tool call. If the model produced no text
     * after its last tool call, all the text it produced is returned instead.
     */
    public String getText() {
        StringBuilder finalText = new StringBuilder();
        for (ModelOutput output : outputs) {
            if (output instanceof ToolCallContent) {
                finalText.setLength(0);
            } else if (output instanceof OutputTextContent text && text.getText() != null) {
                finalText.append(text.getText());
            }
        }
        if (!finalText.isEmpty()) {
            return finalText.toString();
        }
        return getAllText();
    }

    /**
     * @return All the text the model produced over every turn of the request, including text produced before tool calls.
     */
    public String getAllText() {
        StringBuilder text = new StringBuilder();
        getOutputs(OutputTextContent.class).stream()
                .map(OutputTextContent::getText)
                .filter(Objects::nonNull)
                .forEach(text::append);
        return text.toString();
    }

    /**
     * @return All the thinking the model produced, separated by blank lines.
     */
    public String getThinking() {
        StringJoiner thinking = new StringJoiner("\n\n");
        getOutputs(ThinkingContent.class).stream()
                .map(ThinkingContent::getThinking)
                .filter(Objects::nonNull)
                .forEach(thinking::add);
        return thinking.toString();
    }

    /**
     * @return The reason the model gave for finishing its last turn, or null if the client did not report one.
     */
    public @Nullable ModelFinishReason getFinishReason() {
        List<FinishReasonContent> reasons = getOutputs(FinishReasonContent.class);
        return reasons.isEmpty() ? null : reasons.get(reasons.size() - 1).getReason();
    }

    /**
     * @return The token usage summed over every turn of the request, or null if the client did not report any.
     */
    public @Nullable TokenUtilizationContent getTokenUtilization() {
        List<TokenUtilizationContent> utilizations = getOutputs(TokenUtilizationContent.class);
        if (utilizations.isEmpty()) {
            return null;
        }
        return new TokenUtilizationContent(
                utilizations.stream()
                        .mapToLong(TokenUtilizationContent::getInputTokensUsed)
                        .sum(),
                utilizations.stream()
                        .mapToLong(TokenUtilizationContent::getOutputTokensUsed)
                        .sum(),
                utilizations.stream()
                        .mapToLong(TokenUtilizationContent::getCachedTokensUsed)
                        .sum());
    }
}
