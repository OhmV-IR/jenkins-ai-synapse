package io.ohmvir.plugins.jenkinsaisynapse.configuration;

import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import hudson.security.Permission;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.PromptConfiguration;
import jenkins.model.Jenkins;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConversationHistoryManagementLink extends ManagementLink {
    public List<ModelConversation> getConversations(){
        try {
            Path startPath = Paths.get(Jenkins.get().getRootPath().child("conversations").getRemote());
            try (Stream<Path> stream = Files.walk(startPath)) {
                return stream.filter(Files::isRegularFile)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .map(ConversationHistoryManagementLink::removeExtension)
                        .map(UUID::fromString)
                        .map(conversationId -> {
                            try {
                                return new ModelConversation(conversationId);
                            } catch (IOException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            return List.of();
        }
    }

    public List<Descriptor<PromptConfiguration>> getPromptDescriptors() {
        return Jenkins.get().getDescriptorList(PromptConfiguration.class);
    }

    private static String removeExtension(String fileName){
        int lastDotIdx = fileName.lastIndexOf('.');
        if(lastDotIdx > 0){
            return fileName.substring(0, lastDotIdx);
        }
        return fileName;
    }

    @Override
    public String getIconFileName() {
        return "symbol-settings-outline plugin-ionicons-api";
    }

    @Override
    public String getDisplayName() {
        return "Past Conversations";
    }

    @Override
    public String getUrlName() {
        return "conversations";
    }

    @Override
    public String getDescription() {
        return "View all conversation history for debugging purposes";
    }

    @Override
    public @NonNull Category getCategory() {
        return Category.TROUBLESHOOTING;
    }

    @Override
    public @NonNull Permission getRequiredPermission() {
        return Jenkins.ADMINISTER;
    }
}
