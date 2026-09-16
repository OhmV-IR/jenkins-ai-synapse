package io.ohmvir.plugins.jenkinsaisynapse.api;

import hudson.XmlFile;
import hudson.model.Saveable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import jenkins.model.Jenkins;
import jenkins.model.Loadable;
import lombok.Getter;

public class ModelConversation implements Saveable, Loadable {
    private final @Getter UUID conversationId;
    private final ArrayList<ModelContent> conversation = new ArrayList<>();

    public ModelConversation(UUID conversationId) throws IOException {
        this.conversationId = conversationId;
        load();
    }

    public ModelConversation() {
        conversationId = UUID.randomUUID();
    }

    public void addContent(ModelContent content) throws IOException {
        conversation.add(content);
        save();
    }

    public void addAllContent(Collection<ModelContent> content) throws IOException {
        conversation.addAll(content);
        save();
    }

    public List<ModelContent> getConversation() {
        return Collections.unmodifiableList(conversation);
    }

    protected XmlFile getSaveLocation() {
        return new XmlFile(
                Jenkins.XSTREAM,
                new File(
                        new File(Jenkins.get().getRootDir(), "conversations"),
                        getConversationId().toString() + ".xml"));
    }

    @Override
    public void save() throws IOException {
        getSaveLocation().write(this);
    }

    @Override
    public void load() throws IOException {
        XmlFile configFile = getSaveLocation();
        if (configFile.exists()) {
            configFile.unmarshal(this);
        }
    }
}
