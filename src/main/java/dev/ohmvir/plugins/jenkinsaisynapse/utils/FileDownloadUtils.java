package dev.ohmvir.plugins.jenkinsaisynapse.utils;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.StaplerResponse2;

/**
 * Serves file content that came from (or was sent to) a model. The file name and content type are not trusted, as the
 * model controls them: the file is always sent as an opaque attachment that the browser cannot render on the Jenkins
 * origin. Media that the UI plays inline is sent with its own type, but without sniffing and in a sandbox.
 */
public class FileDownloadUtils {
    static final String DEFAULT_FILE_NAME = "download";
    private static final int MAX_FILE_NAME_LENGTH = 128;
    private static final String SANDBOX_CSP = "sandbox; default-src 'none'";

    public static void serveAttachment(StaplerResponse2 rsp, @Nullable String fileName, byte[] data)
            throws IOException {
        if (data == null || data.length == 0) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        rsp.setContentType("application/octet-stream");
        rsp.setHeader("X-Content-Type-Options", "nosniff");
        rsp.setHeader("Content-Security-Policy", SANDBOX_CSP);
        rsp.setHeader("Content-Disposition", "attachment; filename=\"" + sanitizeFileName(fileName) + "\"");
        rsp.setContentLength(data.length);
        rsp.getOutputStream().write(data);
    }

    /**
     * Serves audio or video for an inline player. The MIME type must already be checked to be a media type: the
     * browser is told not to sniff another type, and the sandbox stops the response from running script if it is
     * opened directly.
     * @param rsp The response to write to
     * @param mimeType The validated media MIME type, e.g. {@code audio/wav} or {@code video/mp4}
     * @param data The media bytes
     */
    public static void serveInlineMedia(StaplerResponse2 rsp, String mimeType, byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        rsp.setContentType(mimeType);
        rsp.setHeader("X-Content-Type-Options", "nosniff");
        rsp.setHeader("Content-Security-Policy", SANDBOX_CSP);
        rsp.setContentLength(data.length);
        rsp.getOutputStream().write(data);
    }

    /**
     * Reduces a file name to a safe, header friendly form: the last path segment, restricted to ASCII letters, digits,
     * '.', '-' and '_', without leading dots, and of bounded length.
     * @param fileName The untrusted file name
     * @return A file name that is safe to put in a Content-Disposition header
     */
    public static String sanitizeFileName(@Nullable String fileName) {
        if (fileName == null) {
            return DEFAULT_FILE_NAME;
        }
        String name = fileName.substring(Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_").replaceFirst("^\\.+", "");
        if (name.length() > MAX_FILE_NAME_LENGTH) {
            name = name.substring(name.length() - MAX_FILE_NAME_LENGTH);
        }
        return name.isEmpty() ? DEFAULT_FILE_NAME : name;
    }
}
