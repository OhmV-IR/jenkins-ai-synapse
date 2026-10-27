package dev.ohmvir.plugins.jenkinsaisynapse.steps;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputAudioContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputFileContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputImageContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputVideoContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputAudioContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputFileContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputImageContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputVideoContent;
import hudson.AbortException;
import hudson.FilePath;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Reads workspace files to attach to a request, and saves the media and files a model produced to the workspace.
 */
final class WorkspaceFiles {
    /** Files larger than this cannot be attached to a request. */
    static final long MAX_ATTACHMENT_BYTES = 32L * 1024 * 1024;

    private static final Map<String, String> CONTENT_TYPES_BY_EXTENSION = Map.ofEntries(
            Map.entry("md", "text/markdown"),
            Map.entry("json", "application/json"),
            Map.entry("yaml", "application/yaml"),
            Map.entry("yml", "application/yaml"),
            Map.entry("xml", "application/xml"),
            Map.entry("csv", "text/csv"),
            Map.entry("html", "text/html"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("webm", "video/webm"),
            Map.entry("mov", "video/quicktime"));

    private WorkspaceFiles() {}

    /**
     * Reads a workspace file as the input matching its content type: an image, audio, video, or any other file.
     *
     * @param path The path of the file, relative to the workspace
     */
    static ModelInput readAttachment(FilePath workspace, String path) throws IOException, InterruptedException {
        FilePath file = workspace.child(path);
        if (!file.exists()) {
            throw new AbortException("File to attach does not exist: " + path);
        }
        if (file.isDirectory()) {
            throw new AbortException("File to attach is a directory: " + path);
        }
        if (file.length() > MAX_ATTACHMENT_BYTES) {
            throw new AbortException("File to attach is larger than " + MAX_ATTACHMENT_BYTES + " bytes: " + path);
        }
        byte[] data;
        try (InputStream in = file.read()) {
            data = in.readAllBytes();
        }
        String contentType = guessContentType(file.getName(), data);
        if (contentType.startsWith("image/")) {
            return new InputImageContent(data, contentType);
        }
        if (contentType.startsWith("audio/")) {
            return new InputAudioContent(data, contentType);
        }
        if (contentType.startsWith("video/")) {
            return new InputVideoContent(data, contentType);
        }
        return new InputFileContent(file.getName(), data, contentType);
    }

    static String guessContentType(String fileName, byte[] data) {
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) {
            String contentType =
                    CONTENT_TYPES_BY_EXTENSION.get(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
            if (contentType != null) {
                return contentType;
            }
        }
        String contentType = URLConnection.guessContentTypeFromName(fileName);
        if (contentType != null) {
            return contentType;
        }
        for (int i = 0; i < Math.min(data.length, 8000); i++) {
            if (data[i] == 0) {
                return "application/octet-stream";
            }
        }
        return "text/plain";
    }

    /**
     * Describes the images, audio, video and files the model produced, saving them to the output directory if there
     * is one.
     *
     * @param outputDirectory Where to save the outputs, or null to only describe them
     * @param outputDir The output directory as given to the step, used for the returned paths
     * @return A map for each output with its {@code name}, {@code kind}, {@code contentType}, {@code size}, and
     *     {@code path} if it was saved
     */
    static List<Map<String, Object>> saveOutputs(
            ModelResponse response, @Nullable FilePath outputDirectory, @Nullable String outputDir)
            throws IOException, InterruptedException {
        List<Map<String, Object>> files = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();
        int index = 0;
        for (ModelOutput output : response.getOutputs()) {
            String kind;
            @Nullable String name = null;
            byte[] data;
            String contentType;
            switch (output) {
                case OutputImageContent image -> {
                    kind = "image";
                    data = image.getImageData();
                    contentType = image.getMimeType();
                }
                case OutputAudioContent audio -> {
                    kind = "audio";
                    data = audio.getAudioData();
                    contentType = audio.getMimeType();
                }
                case OutputVideoContent video -> {
                    kind = "video";
                    data = video.getVideoData();
                    contentType = video.getMimeType();
                }
                case OutputFileContent file -> {
                    kind = "file";
                    name = file.getFileId();
                    data = file.getFileData();
                    contentType = file.getContentType();
                }
                default -> {
                    continue;
                }
            }
            index++;
            if (data == null) {
                data = new byte[0];
            }
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            String fileName = uniqueFileName(
                    name == null ? kind + "-" + index + "." + extensionFor(contentType) : name, index, usedNames);
            Map<String, Object> description = new LinkedHashMap<>();
            description.put("name", fileName);
            description.put("kind", kind);
            description.put("contentType", contentType);
            description.put("size", data.length);
            if (outputDirectory != null) {
                outputDirectory.mkdirs();
                outputDirectory.child(fileName).copyFrom(new ByteArrayInputStream(data));
                description.put("path", outputDir + "/" + fileName);
            }
            files.add(description);
        }
        return files;
    }

    private static String extensionFor(String contentType) {
        String mediaType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return CONTENT_TYPES_BY_EXTENSION.entrySet().stream()
                .filter(entry -> entry.getValue().equals(mediaType))
                .map(Map.Entry::getKey)
                .sorted()
                .findFirst()
                .orElse(mediaType.startsWith("text/") ? "txt" : "bin");
    }

    /**
     * Keeps model chosen file names from escaping the output directory or overwriting each other.
     */
    static String uniqueFileName(@Nullable String name, int index, Set<String> usedNames) {
        String fileName = name == null ? "" : name;
        fileName = fileName.substring(Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')) + 1);
        fileName = fileName.replaceAll("[^A-Za-z0-9._-]", "_").replaceFirst("^\\.+", "");
        if (fileName.isEmpty()) {
            fileName = "output-" + index;
        }
        if (!usedNames.add(fileName)) {
            fileName = index + "-" + fileName;
            usedNames.add(fileName);
        }
        return fileName;
    }
}
