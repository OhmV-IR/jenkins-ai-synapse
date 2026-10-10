package io.ohmvir.plugins.jenkinsaisynapse.api.message;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MediaPartTest {
    private static final byte[] DATA = "not-really-an-image".getBytes(StandardCharsets.UTF_8);

    @Test
    void keepsBytesAndMimeType() {
        MediaPart image = new MediaPart(MediaPart.Kind.IMAGE, null, "image/jpeg", DATA);
        assertArrayEquals(DATA, image.getData());
        assertEquals("image/jpeg", image.getMediaType());
        assertEquals("data:image/jpeg;base64,bm90LXJlYWxseS1hbi1pbWFnZQ==", image.getDataUri());

        MediaPart audio = new MediaPart(MediaPart.Kind.AUDIO, null, " audio/ogg; codecs=opus ", DATA);
        assertEquals("audio/ogg; codecs=opus", audio.getMediaType());

        MediaPart video = new MediaPart(MediaPart.Kind.VIDEO, null, "video/mp4; codecs=\"avc1.42E01E\"", DATA);
        assertEquals("video/mp4; codecs=\"avc1.42E01E\"", video.getMediaType());

        MediaPart file = new MediaPart(MediaPart.Kind.FILE, "report.pdf", "application/pdf", DATA);
        assertEquals("report.pdf", file.getName());
        assertEquals(DATA.length, file.getSize());
    }

    @Test
    void copiesItsBytes() {
        byte[] data = {1, 2, 3};
        MediaPart part = new MediaPart(MediaPart.Kind.FILE, null, "application/octet-stream", data);
        data[0] = 9;
        part.getData()[1] = 9;
        assertArrayEquals(new byte[] {1, 2, 3}, part.getData());
    }

    @Test
    void emptyMediaHasNoDataUri() {
        assertNull(new MediaPart(MediaPart.Kind.IMAGE, null, "image/png", new byte[0]).getDataUri());
    }

    @Test
    void rejectsMimeTypesThatDoNotMatchTheKind() {
        assertThrows(IllegalArgumentException.class, () -> new MediaPart(MediaPart.Kind.IMAGE, null, null, DATA));
        assertThrows(
                IllegalArgumentException.class, () -> new MediaPart(MediaPart.Kind.IMAGE, null, "text/html", DATA));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MediaPart(MediaPart.Kind.IMAGE, null, "application/pdf", DATA));
        assertThrows(IllegalArgumentException.class, () -> new MediaPart(MediaPart.Kind.AUDIO, null, "", DATA));
        assertThrows(
                IllegalArgumentException.class, () -> new MediaPart(MediaPart.Kind.AUDIO, null, "video/mp4", DATA));
        String[] invalidVideoTypes = {"", " ", "text/html", "image/png", "video/", "video/mp4\r\nX-Injected: 1"};
        for (String mimeType : invalidVideoTypes) {
            assertThrows(
                    IllegalArgumentException.class, () -> new MediaPart(MediaPart.Kind.VIDEO, null, mimeType, DATA));
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> new MediaPart(MediaPart.Kind.FILE, null, "text/plain\r\nX-Injected: 1", DATA));
    }
}
