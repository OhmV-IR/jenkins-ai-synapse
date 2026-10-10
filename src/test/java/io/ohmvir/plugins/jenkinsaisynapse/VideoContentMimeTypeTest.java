package io.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputVideoContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputVideoContent;
import org.junit.jupiter.api.Test;

public class VideoContentMimeTypeTest {
    private static final byte[] DATA = {1, 2, 3};

    @Test
    public void inputVideoKeepsDataAndMimeType() {
        InputVideoContent content = new InputVideoContent(DATA, "video/webm");
        assertArrayEquals(DATA, content.getVideoData());
        assertEquals("video/webm", content.getMimeType());
    }

    @Test
    public void outputVideoKeepsDataAndMimeType() {
        OutputVideoContent content = new OutputVideoContent(DATA, "video/mp4; codecs=\"avc1.42E01E\"");
        assertArrayEquals(DATA, content.getVideoData());
        assertEquals("video/mp4; codecs=\"avc1.42E01E\"", content.getMimeType());
    }

    @Test
    public void missingOrInvalidMimeTypeIsRejected() {
        String[] invalidMimeTypes = {null, "", " ", "text/html", "image/png", "video/", "video/mp4\r\nX-Injected: 1"};
        for (String mimeType : invalidMimeTypes) {
            assertThrows(IllegalArgumentException.class, () -> new InputVideoContent(DATA, mimeType));
            assertThrows(IllegalArgumentException.class, () -> new OutputVideoContent(DATA, mimeType));
        }
    }
}
