package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.XmlFile;
import hudson.model.Saveable;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import jenkins.model.Jenkins;
import jenkins.model.Loadable;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ModelConversation implements Saveable, Loadable {
    private final @Getter UUID conversationId;
    private final ArrayList<ModelRequest> requests = new ArrayList<>();
    private final ArrayList<ModelResponse> responses = new ArrayList<>();
    public ModelConversation(UUID conversationId) throws IOException {
        this.conversationId = conversationId;
        load();
    }
    public ModelConversation() {
        conversationId = UUID.randomUUID();
    }

    public void addRequest(ModelRequest request) throws IOException {
        requests.add(request);
        save();
    }

    public void addResponse(ModelResponse response) throws IOException {
        responses.add(response);
        save();
    }

    public List<ModelRequest> getRequests() {
        return Collections.unmodifiableList(requests);
    }

    public List<ModelResponse> getResponses() {
        return Collections.unmodifiableList(responses);
    }

    protected XmlFile getSaveLocation(){
        return new XmlFile(Jenkins.XSTREAM, new File(new File(Jenkins.get().getRootDir(), "conversations"), getConversationId().toString() + ".xml"));
    }

    @Override
    public void save() throws IOException {
        getSaveLocation().write(this);
    }

    @Override
    public void load() throws IOException {
        XmlFile configFile = getSaveLocation();
        if(configFile.exists()){
            configFile.unmarshal(this);
        }
    }
}
