package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputImageContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputImageContent;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class ImageContentTest {

    private static final byte[] DATA = "not-really-an-image".getBytes(StandardCharsets.UTF_8);

    @Test
    public void inputImageKeepsBytesAndMimeType() {
        InputImageContent content = new InputImageContent(DATA, "image/jpeg");
        assertArrayEquals(DATA, content.getImageData());
        assertEquals("image/jpeg", content.getMimeType());
        assertEquals("data:image/jpeg;base64,bm90LXJlYWxseS1hbi1pbWFnZQ==", content.getImageDataUri());
    }

    @Test
    public void outputImageKeepsBytesAndMimeType() {
        OutputImageContent content = new OutputImageContent(DATA, "image/webp");
        assertArrayEquals(DATA, content.getImageData());
        assertEquals("image/webp", content.getMimeType());
        assertEquals("data:image/webp;base64,bm90LXJlYWxseS1hbi1pbWFnZQ==", content.getImageDataUri());
    }

    @Test
    public void emptyImageHasNoDataUri() {
        assertNull(new InputImageContent(null, "image/png").getImageDataUri());
        assertNull(new OutputImageContent(new byte[0], "image/png").getImageDataUri());
    }

    @Test
    public void rejectsNonImageMimeTypes() {
        assertThrows(IllegalArgumentException.class, () -> new InputImageContent(DATA, null));
        assertThrows(IllegalArgumentException.class, () -> new InputImageContent(DATA, "text/html"));
        assertThrows(IllegalArgumentException.class, () -> new OutputImageContent(DATA, "application/pdf"));
    }
}
