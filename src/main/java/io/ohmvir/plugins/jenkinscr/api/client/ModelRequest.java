package io.ohmvir.plugins.jenkinscr.api.client;

import io.ohmvir.plugins.jenkinscr.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinscr.configuration.agents.AgentConfiguration;

import java.util.UUID;

public abstract class ModelRequest implements Cloneable {
    private final AgentConfiguration agentConfiguration;
    public ModelRequest(AgentConfiguration agentConfiguration){
        this.agentConfiguration = agentConfiguration;
    }
    public abstract UUID AttachFile(byte[] fileBytes);
    public abstract void RemoveFile(UUID fileId);
    public abstract UUID[] GetFiles();
    /** Appends text to the prompt. */
    public abstract void AttachText(String text);
    /** Gets the current prompt text. Does not include the system prompt */
    public abstract String GetText();
    /** Sets the full prompt text */
    public abstract void SetText(String newPrompt);
    /** Attaches the conversation history to the request */
    public abstract void AddConversationHistory(ModelConversation conversation);
    public abstract void RequestOutputType(ModelOutputType outputType);
  //  public abstract void AttachTool(ToolData tool); future TODO
  //  public abstract void AttachSkill(SkillData skill); future TODO

    /**
     * For convenience, generally flow will be to make a base request with all tools, skills and such and then clone it and add
     * the specific prompt text
     * @return A shallow clone of the prompt. File byte data will be copied, but Skills and tool data which are stored in classes will continue to refer to the old data.
     */
    @Override
    public abstract ModelRequest clone();
}
