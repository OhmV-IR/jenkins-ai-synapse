package io.ohmvir.plugins.jenkinsaisynapse.api;

import hudson.XmlFile;
import hudson.model.Saveable;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.Message;
import java.io.File;
import java.io.IOException;
import java.util.*;
import jenkins.model.Jenkins;
import jenkins.model.Loadable;
import lombok.Getter;

public class ModelConversation implements Saveable, Loadable {
    private final @Getter UUID conversationId;
    private final ArrayList<Message> conversation = new ArrayList<>();

    public ModelConversation(UUID conversationId) throws IOException {
        this.conversationId = conversationId;
        load();
    }

    public ModelConversation() {
        conversationId = UUID.randomUUID();
    }

    public synchronized void addMessage(Message message) throws IOException {
        conversation.add(message);
        save();
    }

    public synchronized void addMessages(Collection<Message> messages) throws IOException {
        conversation.addAll(messages);
        save();
    }

    /**
     * @return The conversation's messages, oldest first. Prepend them to a request to continue the conversation.
     */
    public synchronized List<Message> getMessages() {
        return List.copyOf(conversation);
    }

    protected XmlFile getSaveLocation() {
        return new XmlFile(
                Jenkins.XSTREAM,
                new File(
                        new File(Jenkins.get().getRootDir(), "conversations"),
                        getConversationId().toString() + ".xml"));
    }

    @Override
    public synchronized void save() throws IOException {
        getSaveLocation().write(this);
    }

    @Override
    public synchronized void load() throws IOException {
        XmlFile configFile = getSaveLocation();
        if (configFile.exists()) {
            configFile.unmarshal(this);
        }
    }
}
