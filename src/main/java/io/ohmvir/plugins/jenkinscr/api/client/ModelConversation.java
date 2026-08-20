package io.ohmvir.plugins.jenkinscr.api.client;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ModelConversation {
    public ModelConversation(UUID conversationId){
        this.conversationId = conversationId;
    }
    public ModelConversation(){
        conversationId = UUID.randomUUID();
    }
    private final @Getter UUID conversationId;
    private final ArrayList<ModelRequest> requests = new ArrayList<>();
    private final ArrayList<ModelResponse> responses = new ArrayList<>();

    public void AddRequest(ModelRequest request){
        requests.add(request);
    }
    public void AddResponse(ModelResponse response){
        responses.add(response);
    }

    public List<ModelRequest> getRequests(){
        return Collections.unmodifiableList(requests);
    }

    public List<ModelResponse> getResponses(){
        return Collections.unmodifiableList(responses);
    }
}
