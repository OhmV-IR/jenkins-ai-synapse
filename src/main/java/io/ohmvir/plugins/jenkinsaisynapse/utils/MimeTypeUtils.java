package io.ohmvir.plugins.jenkinsaisynapse.utils;

import java.util.regex.Pattern;

public class MimeTypeUtils {
    private static final String TOKEN = "[A-Za-z0-9!#$&^_.+-]+";
    private static final Pattern VIDEO_MIME_TYPE =
            Pattern.compile("(?i)video/" + TOKEN + "(\\s*;\\s*" + TOKEN + "=(" + TOKEN + "|\"[^\"\\p{Cntrl}]*\"))*");

    /**
     * Validates that a MIME type identifies a video format, eg {@code video/mp4} or {@code video/webm; codecs="vp9"}.
     * @param mimeType The MIME type to validate
     * @param field The name of the parameter the MIME type was passed as, used in the error message
     * @return The validated MIME type
     * @throws IllegalArgumentException If the MIME type is missing or is not a valid video MIME type
     */
    public static String requireVideoMimeType(String mimeType, String field) {
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException(field + ": video MIME type is required");
        }
        if (!VIDEO_MIME_TYPE.matcher(mimeType).matches()) {
            throw new IllegalArgumentException(field + ": invalid video MIME type: " + mimeType);
        }
        return mimeType;
    }
}
