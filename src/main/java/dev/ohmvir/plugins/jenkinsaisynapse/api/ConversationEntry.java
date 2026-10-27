package dev.ohmvir.plugins.jenkinsaisynapse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputAudioContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputFileContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputImageContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputSkillContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputToolContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputVideoContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.MaxOutputTokensContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.StopSequencesContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.SystemPromptContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TemperatureContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ThinkingLevelContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ToolCallResponseContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TopKContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TopPContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.CitationContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.FinishReasonContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputAudioContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputFileContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputImageContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputVideoContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ThinkingContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.TokenUtilizationContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * A stored record of one piece of a {@link ModelConversation}. Only plain values are kept: text is truncated to
 * {@link #MAX_TEXT_LENGTH} characters, and binary content (images, audio, video, files) is recorded by MIME type and
 * size only.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationEntry {
    public static final int MAX_TEXT_LENGTH = 100_000;

    public static final String DIRECTION_INPUT = "input";
    public static final String DIRECTION_OUTPUT = "output";

    /** {@link #DIRECTION_INPUT} for content sent to the model, {@link #DIRECTION_OUTPUT} for content it produced. */
    private String direction;
    /** Simple name of the content class, for example {@code OutputTextContent}. */
    private String type;

    private @Nullable String text;
    private @Nullable String toolName;
    private @Nullable String toolUseId;
    private @Nullable Boolean successful;
    private @Nullable String mimeType;
    private @Nullable Long sizeBytes;

    public static ConversationEntry from(ModelContent content) {
        ConversationEntry entry = new ConversationEntry();
        entry.direction = content instanceof ModelInput ? DIRECTION_INPUT : DIRECTION_OUTPUT;
        entry.type = content.getClass().getSimpleName();
        switch (content) {
            case InputTextContent c -> entry.setText(c.getText());
            case SystemPromptContent c -> entry.setText(c.getSystemPrompt());
            case TemperatureContent c -> entry.setText(String.valueOf(c.getTemperature()));
            case MaxOutputTokensContent c -> entry.setText(String.valueOf(c.getMaxOutputTokens()));
            case ThinkingLevelContent c -> entry.setText(String.valueOf(c.getThinkingLevel()));
            case TopPContent c -> entry.setText(String.valueOf(c.getTopP()));
            case TopKContent c -> entry.setText(String.valueOf(c.getTopK()));
            case StopSequencesContent c -> entry.setText(String.valueOf(c.getStopPhrases()));
            case InputToolContent c ->
                entry.toolName = c.getTool() == null ? null : c.getTool().getName();
            case InputSkillContent c ->
                entry.setText(c.getSkill() == null ? null : c.getSkill().getSkillName());
            case ToolCallResponseContent c -> {
                entry.toolUseId = c.getToolUseId();
                entry.toolName =
                        c.getCalledTool() == null ? null : c.getCalledTool().getName();
                entry.successful = c.isSuccessful();
                entry.setText(c.getResponseContent());
            }
            case InputImageContent c -> entry.setBinary(c.getMimeType(), c.getImageData());
            case InputAudioContent c -> entry.setBinary(c.getMimeType(), c.getAudioData());
            case InputVideoContent c -> entry.setBinary(c.getMimeType(), c.getVideoData());
            case InputFileContent c -> entry.setBinary(c.getContentType(), c.getFileData());
            case OutputTextContent c -> entry.setText(c.getText());
            case ThinkingContent c -> entry.setText(c.getThinking());
            case ToolCallContent c -> {
                entry.toolUseId = c.getToolUseId();
                entry.toolName = c.getName();
                entry.setText(c.getArgumentsPrettyJson());
            }
            case TokenUtilizationContent c ->
                entry.setText("input tokens: " + c.getInputTokensUsed() + ", output tokens: " + c.getOutputTokensUsed()
                        + ", cached tokens: " + c.getCachedTokensUsed());
            case FinishReasonContent c -> entry.setText(String.valueOf(c.getReason()));
            case CitationContent c ->
                entry.setText(c.getCitedText() + "\ndocument " + c.getDocumentIndex()
                        + (c.getDocumentTitle() == null ? "" : " (" + c.getDocumentTitle() + ")")
                        + (c.getFileId() == null ? "" : ", file " + c.getFileId())
                        + ", characters " + c.getStartCharIndex() + "-" + c.getEndCharIndex());
            case OutputImageContent c -> entry.setBinary(c.getMimeType(), c.getImageData());
            case OutputAudioContent c -> entry.setBinary(c.getMimeType(), c.getAudioData());
            case OutputVideoContent c -> entry.setBinary(c.getMimeType(), c.getVideoData());
            case OutputFileContent c -> entry.setBinary(c.getContentType(), c.getFileData());
            default -> {}
        }
        return entry;
    }

    public void setText(@Nullable String text) {
        if (text != null && text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH) + "\n[truncated " + (text.length() - MAX_TEXT_LENGTH)
                    + " characters]";
        }
        this.text = text;
    }

    private void setBinary(@Nullable String mimeType, byte[] data) {
        this.mimeType = mimeType;
        this.sizeBytes = data == null ? 0L : (long) data.length;
    }
}
