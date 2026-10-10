package io.ohmvir.plugins.jenkinsaisynapse.utils;

import hudson.model.Descriptor;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

public class MimeTypeUtils {
    private static final String TOKEN = "[A-Za-z0-9!#$&^_.+-]+";
    private static final String PARAMETERS = "(\\s*;\\s*" + TOKEN + "=(" + TOKEN + "|\"[^\"\\p{Cntrl}]*\"))*";
    private static final Pattern MIME_TYPE = Pattern.compile("(" + TOKEN + ")/" + TOKEN + PARAMETERS);

    /**
     * Checks that a value is a well-formed MIME type, optionally of a given top-level type, eg {@code video/mp4} or
     * {@code audio/ogg; codecs=opus}. Values with control characters, such as header injection attempts, are rejected.
     *
     * @param mimeType The MIME type to check
     * @param topLevelType The required top-level type, such as {@code video}, compared case-insensitively; or null to
     *                     accept any
     */
    public static boolean isMimeType(@Nullable String mimeType, @Nullable String topLevelType) {
        if (mimeType == null) {
            return false;
        }
        var matcher = MIME_TYPE.matcher(mimeType);
        return matcher.matches() && (topLevelType == null || matcher.group(1).equalsIgnoreCase(topLevelType));
    }

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
        if (!isMimeType(mimeType, "video")) {
            throw new Descriptor.FormException("Invalid video MIME type: " + mimeType, field);
        }
        return mimeType;
    }
}
