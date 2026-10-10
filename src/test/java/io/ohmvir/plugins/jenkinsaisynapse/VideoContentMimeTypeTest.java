package io.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        OutputVideoContent content = new OutputVideoContent(DATA, "video/quicktime");
        assertArrayEquals(DATA, content.getVideoData());
        assertEquals("video/quicktime", content.getMimeType());
    }

    @Test
    public void missingOrNonVideoMimeTypeFallsBackToDefault() {
        assertEquals(InputVideoContent.DEFAULT_MIME_TYPE, new InputVideoContent(DATA, null).getMimeType());
        assertEquals(InputVideoContent.DEFAULT_MIME_TYPE, new InputVideoContent(DATA, "text/html").getMimeType());
        assertEquals(OutputVideoContent.DEFAULT_MIME_TYPE, new OutputVideoContent(DATA, " ").getMimeType());
        assertEquals(OutputVideoContent.DEFAULT_MIME_TYPE, new OutputVideoContent(DATA, "image/png").getMimeType());
    }
}
