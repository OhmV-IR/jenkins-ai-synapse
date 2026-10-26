package dev.ohmvir.plugins.jenkinsaisynapse.utils;

import hudson.model.Descriptor;
import java.util.regex.Pattern;

public class MimeTypeUtils {
    private static final String TOKEN = "[A-Za-z0-9!#$&^_.+-]+";
    private static final Pattern VIDEO_MIME_TYPE =
            Pattern.compile("(?i)video/" + TOKEN + "(\\s*;\\s*" + TOKEN + "=(" + TOKEN + "|\"[^\"\\p{Cntrl}]*\"))*");

    /**
     * Validates that a MIME type identifies a video format, eg {@code video/mp4} or {@code video/webm; codecs="vp9"}.
     * @param mimeType The MIME type to validate
     * @param field The form field the MIME type was submitted through
     * @return The validated MIME type
     * @throws Descriptor.FormException If the MIME type is missing or is not a valid video MIME type
     */
    public static String requireVideoMimeType(String mimeType, String field) throws Descriptor.FormException {
        if (mimeType == null || mimeType.isBlank()) {
            throw new Descriptor.FormException("Video MIME type is required", field);
        }
        if (!VIDEO_MIME_TYPE.matcher(mimeType).matches()) {
            throw new Descriptor.FormException("Invalid video MIME type: " + mimeType, field);
        }
        return mimeType;
    }
}
