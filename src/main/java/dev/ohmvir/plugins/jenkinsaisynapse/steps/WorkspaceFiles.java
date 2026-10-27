package dev.ohmvir.plugins.jenkinsaisynapse.steps;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputAudioContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputFileContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputImageContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputVideoContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputAudioContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputFileContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputImageContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputVideoContent;
import dev.ohmvir.plugins.jenkinsaisynapse.utils.FileDownloadUtils;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Util;
import hudson.remoting.VirtualChannel;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import jenkins.MasterToSlaveFileCallable;
import org.jspecify.annotations.Nullable;

/**
 * Reads workspace files to attach to a request, and saves the media and files a model produced to the workspace.
 */
final class WorkspaceFiles {
    /** Files larger than this cannot be attached to a request. */
    static final long MAX_ATTACHMENT_BYTES = 32L * 1024 * 1024;

    /**
     * All the files attached to one request together cannot be larger than this. They are held in controller memory
     * (and again once a provider base64 encodes them), so this is kept to two maximum size files.
     */
    static final long MAX_TOTAL_ATTACHMENT_BYTES = 64L * 1024 * 1024;

    private static final Map<String, String> CONTENT_TYPES_BY_EXTENSION = Map.ofEntries(
            Map.entry("txt", "text/plain"),
            Map.entry("md", "text/markdown"),
            Map.entry("json", "application/json"),
            Map.entry("yaml", "application/yaml"),
            Map.entry("yml", "application/yaml"),
            Map.entry("xml", "application/xml"),
            Map.entry("csv", "text/csv"),
            Map.entry("html", "text/html"),
            Map.entry("svg", "image/svg+xml"),
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

    /** The only image types providers accept as image input, other images such as SVG or BMP are attached as files. */
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");

    /** Textual types that do not start with {@code text/}, they are sent as text rather than as a file. */
    private static final Set<String> TEXTUAL_APPLICATION_TYPES = Set.of(
            "application/json",
            "application/yaml",
            "application/x-yaml",
            "application/xml",
            "application/toml",
            "application/javascript",
            "application/sql",
            "application/x-sh",
            "image/svg+xml");

    /** Device names that cannot be used as a file name on Windows, with or without an extension. */
    private static final Pattern WINDOWS_DEVICE_NAME =
            Pattern.compile("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?");

    /** What follows the last dot of a file name for it to count as an extension, see {@code hasExtension}. */
    private static final Pattern EXTENSION = Pattern.compile("(?=[0-9]*[A-Za-z])[A-Za-z0-9]{1,10}");

    /** The longest output file name, the same as the limit applied by {@code FileDownloadUtils.sanitizeFileName}. */
    private static final int MAX_FILE_NAME_LENGTH = 128;

    private WorkspaceFiles() {}

    /**
     * Keeps track of the size of the files attached to one request so that attaching one more file can be refused
     * before it is read, once the files would together be larger than the limit.
     */
    static final class AttachmentBudget {
        private final long limitBytes;
        private long usedBytes;

        AttachmentBudget() {
            this(MAX_TOTAL_ATTACHMENT_BYTES);
        }

        AttachmentBudget(long limitBytes) {
            this.limitBytes = limitBytes;
        }

        /**
         * Counts a file towards the total.
         *
         * @param path The path of the file, for the error message
         * @param length The size of the file in bytes
         * @throws AbortException If the files attached so far and this one are larger than the limit
         */
        void add(String path, long length) throws AbortException {
            if (length > limitBytes - usedBytes) {
                throw new AbortException("Files to attach are larger than " + limitBytes / (1024 * 1024)
                        + " MiB in total (" + limitBytes + " bytes), attaching this file would exceed it: " + path);
            }
            usedBytes += length;
        }

        /**
         * @return How many more bytes can be attached before the limit is reached
         */
        long remaining() {
            return limitBytes - usedBytes;
        }
    }

    /**
     * The result of {@link ReadFile}.
     *
     * @param data The content of the file, or null if the file is larger than the total size still allowed
     * @param length The size of the file in bytes, larger than the allowance if there is no data
     */
    private record FileContent(byte @Nullable [] data, long length) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    /**
     * Checks and reads a file in a single call to the agent, rather than one remoting round trip for each of exists,
     * isDirectory, length and read. The read is bounded so that a file that is too large is never held in memory.
     */
    private static final class ReadFile extends MasterToSlaveFileCallable<FileContent> {
        @Serial
        private static final long serialVersionUID = 1L;

        private final String path;
        private final long remainingBytes;

        ReadFile(String path, long remainingBytes) {
            this.path = path;
            this.remainingBytes = remainingBytes;
        }

        @Override
        public FileContent invoke(File file, VirtualChannel channel) throws IOException {
            if (!file.exists()) {
                throw new AbortException("File to attach does not exist: " + path);
            }
            if (file.isDirectory()) {
                throw new AbortException("File to attach is a directory: " + path);
            }
            long length = file.length();
            if (length > MAX_ATTACHMENT_BYTES) {
                throw new AbortException(tooLargeMessage(path));
            }
            if (length > remainingBytes) {
                return new FileContent(null, length);
            }
            // The file can grow after its length was checked, so the read itself is bounded, by one more byte than
            // allowed to tell that it went over.
            long allowedBytes = Math.min(MAX_ATTACHMENT_BYTES, remainingBytes);
            byte[] data;
            try (InputStream in = Files.newInputStream(file.toPath())) {
                data = in.readNBytes((int) allowedBytes + 1);
            }
            if (data.length > allowedBytes) {
                if (allowedBytes == MAX_ATTACHMENT_BYTES) {
                    throw new AbortException(tooLargeMessage(path));
                }
                return new FileContent(null, data.length);
            }
            return new FileContent(data, data.length);
        }
    }

    private static String tooLargeMessage(String path) {
        return "File to attach is larger than " + MAX_ATTACHMENT_BYTES + " bytes: " + path;
    }

    /**
     * Reads a single workspace file, see {@link #readAttachment(FilePath, String, AttachmentBudget)}.
     */
    static ModelInput readAttachment(FilePath workspace, @Nullable String path)
            throws IOException, InterruptedException {
        return readAttachment(workspace, path, new AttachmentBudget());
    }

    /**
     * Reads a workspace file as the input matching its content type: a PNG, JPEG, GIF or WebP image, audio, video,
     * text (including JSON, YAML, XML and SVG) headed by the file name, or any other file such as a PDF.
     *
     * @param path The path of the file, relative to the workspace
     * @param budget The total size of the files attached to the request, a file larger than what is still allowed is
     *     not read
     * @throws AbortException If the path is missing or blank, or the file cannot be attached
     */
    static ModelInput readAttachment(FilePath workspace, @Nullable String path, AttachmentBudget budget)
            throws IOException, InterruptedException {
        if (Util.fixEmptyAndTrim(path) == null) {
            throw new AbortException("File to attach has no path, check for an empty or unset variable in files");
        }
        FilePath file = workspace.child(path);
        FileContent content = file.act(new ReadFile(path, budget.remaining()));
        // A file larger than the total size still allowed comes back without data, and is always refused here
        budget.add(path, content.length());
        byte[] data = content.data();
        if (data == null) {
            throw new AbortException("File to attach could not be read: " + path);
        }
        String contentType = guessContentType(file.getName(), data);
        if (IMAGE_CONTENT_TYPES.contains(contentType)) {
            return new InputImageContent(data, contentType);
        }
        if (contentType.startsWith("audio/")) {
            return new InputAudioContent(data, contentType);
        }
        if (contentType.startsWith("video/")) {
            return new InputVideoContent(data, contentType);
        }
        if (isTextual(contentType)) {
            return new InputTextContent("File " + file.getName() + ":\n" + new String(data, StandardCharsets.UTF_8));
        }
        return new InputFileContent(file.getName(), data, contentType);
    }

    private static boolean isTextual(String contentType) {
        return contentType.startsWith("text/")
                || TEXTUAL_APPLICATION_TYPES.contains(contentType)
                || contentType.endsWith("+json")
                || contentType.endsWith("+xml");
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
     * is one. An output is saved under the file name the model gave it, or the provider's file id for a file. A name
     * without an extension, such as an id, gets the one for the content type of the output, and an output without a
     * name is called after its kind and position, such as {@code image-1.png}.
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
        if (outputDirectory != null && !response.getOutputs().isEmpty()) {
            outputDirectory.mkdirs();
        }
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
            String extension = extensionFor(contentType);
            String fileName = uniqueFileName(name, kind + "-" + index + "." + extension, extension, index, usedNames);
            Map<String, Object> description = new LinkedHashMap<>();
            description.put("name", fileName);
            description.put("kind", kind);
            description.put("contentType", contentType);
            description.put("size", data.length);
            if (outputDirectory != null) {
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
     * Same as {@link #uniqueFileName(String, String, String, int, Set)}, without adding an extension to a name that
     * has none.
     */
    static String uniqueFileName(@Nullable String name, String fallback, int index, Set<String> usedNames) {
        return uniqueFileName(name, fallback, null, index, usedNames);
    }

    /**
     * Keeps model chosen file names from escaping the output directory, being too long for the file system, being
     * reserved on Windows, or overwriting each other.
     *
     * @param name The file name the model chose, if any
     * @param fallback The file name to use when the model's name has nothing usable in it, trusted to be safe
     * @param extension The extension, without the dot, to add to the model's name when it has none, such as the one
     *     for the content type of a file whose name is only an id. Null to leave the name as it is
     * @param index The position of the output, to tell apart outputs with the same name
     * @param usedNames The names already used in the output directory, this name is added to it. Names are stored in
     *     lower case, as the output directory can be on a case-insensitive file system
     * @return A file name of at most 128 characters that is not used yet, ignoring case
     */
    static String uniqueFileName(
            @Nullable String name, String fallback, @Nullable String extension, int index, Set<String> usedNames) {
        String fileName = FileDownloadUtils.sanitizeFileName(name, fallback);
        if (extension != null && !hasExtension(fileName)) {
            fileName = withExtension(fileName, extension);
        }
        if (WINDOWS_DEVICE_NAME.matcher(fileName).matches()) {
            fileName = "file-" + fileName;
        }
        if (usedNames.add(fileName.toLowerCase(Locale.ROOT))) {
            return fileName;
        }
        // <index>-<name> first, then <index>-<n>-<name> until the name is not used by another output
        for (int attempt = 1; ; attempt++) {
            String prefix = attempt == 1 ? index + "-" : index + "-" + attempt + "-";
            String candidate = withPrefix(prefix, fileName);
            if (usedNames.add(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }
    }

    /**
     * Tells whether a file name ends in something that looks like an extension: a dot after the first character,
     * followed by up to 10 letters and digits of which at least one is a letter. A version number such as
     * {@code model-1.5} is not an extension.
     */
    private static boolean hasExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 && EXTENSION.matcher(fileName.substring(dot + 1)).matches();
    }

    /**
     * Adds an extension to a file name, keeping the result within the length limit by shortening the name before it.
     */
    private static String withExtension(String fileName, String extension) {
        String suffix = "." + extension;
        int stemLength = Math.min(fileName.length(), Math.max(1, MAX_FILE_NAME_LENGTH - suffix.length()));
        // A cut or a trailing dot in the name would leave two dots in a row
        String stem = fileName.substring(0, stemLength).replaceFirst("\\.+$", "");
        return (stem.isEmpty() ? "file" : stem) + suffix;
    }

    /**
     * Adds a prefix to a file name, keeping the result within the length limit by shortening the name before its
     * extension.
     */
    private static String withPrefix(String prefix, String fileName) {
        if (prefix.length() + fileName.length() <= MAX_FILE_NAME_LENGTH) {
            return prefix + fileName;
        }
        int dot = fileName.lastIndexOf('.');
        String extension = dot > 0 ? fileName.substring(dot) : "";
        if (prefix.length() + extension.length() >= MAX_FILE_NAME_LENGTH) {
            // No room left for a name next to the extension, so the extension is shortened with it
            return (prefix + fileName).substring(0, MAX_FILE_NAME_LENGTH);
        }
        String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
        return prefix + stem.substring(0, MAX_FILE_NAME_LENGTH - prefix.length() - extension.length()) + extension;
    }
}
