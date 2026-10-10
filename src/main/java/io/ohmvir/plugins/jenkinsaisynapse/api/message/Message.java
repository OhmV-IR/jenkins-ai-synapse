package io.ohmvir.plugins.jenkinsaisynapse.api.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * One turn of a conversation: who it comes from and what it contains. Messages are immutable.
 * <p>
 * A valid transcript alternates between {@link Role#USER} or {@link Role#TOOL} messages and {@link Role#ASSISTANT}
 * messages, and every assistant message containing {@link ToolCallPart}s is directly followed by a {@link Role#TOOL}
 * message answering each of them.
 */
public final class Message {
    private final Role role;
    // A plain ArrayList, so XStream can persist messages without reflective access to JDK internals.
    private final ArrayList<ContentPart> parts;

    public Message(Role role, List<? extends ContentPart> parts) {
        this.role = Objects.requireNonNull(role, "role");
        List<ContentPart> copy = new ArrayList<>(Objects.requireNonNull(parts, "parts"));
        if (copy.contains(null)) {
            throw new IllegalArgumentException("parts must not contain null");
        }
        for (ContentPart part : copy) {
            if (part instanceof ToolResultPart != (role == Role.TOOL)) {
                throw new IllegalArgumentException("Tool results belong in TOOL messages, and only there");
            }
            if (part instanceof ToolCallPart && role != Role.ASSISTANT) {
                throw new IllegalArgumentException("Only ASSISTANT messages can call tools");
            }
        }
        this.parts = new ArrayList<>(copy);
    }

    public static Message user(String text) {
        return new Message(Role.USER, List.of(new TextPart(text)));
    }

    public static Message user(ContentPart... parts) {
        return new Message(Role.USER, Arrays.asList(parts));
    }

    public static Message assistant(String text) {
        return new Message(Role.ASSISTANT, List.of(new TextPart(text)));
    }

    public static Message assistant(ContentPart... parts) {
        return new Message(Role.ASSISTANT, Arrays.asList(parts));
    }

    public static Message toolResults(List<ToolResultPart> results) {
        return new Message(Role.TOOL, results);
    }

    public Role getRole() {
        return role;
    }

    public List<ContentPart> getParts() {
        return Collections.unmodifiableList(parts);
    }

    public <T extends ContentPart> List<T> getParts(Class<T> type) {
        return parts.stream().filter(type::isInstance).map(type::cast).toList();
    }

    /**
     * @return The text parts of the message, concatenated.
     */
    public String getText() {
        return getParts(TextPart.class).stream().map(TextPart::getText).collect(Collectors.joining());
    }

    public List<ToolCallPart> getToolCalls() {
        return getParts(ToolCallPart.class);
    }

    /**
     * @return A copy of this message with the given parts added at the end.
     */
    public Message withAddedParts(List<? extends ContentPart> added) {
        List<ContentPart> combined = new ArrayList<>(parts);
        combined.addAll(added);
        return new Message(role, combined);
    }

    @Override
    public String toString() {
        return role + parts.toString();
    }
}
