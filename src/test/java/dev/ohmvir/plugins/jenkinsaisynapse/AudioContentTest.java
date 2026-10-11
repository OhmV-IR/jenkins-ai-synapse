package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputAudioContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputAudioContent;
import org.junit.jupiter.api.Test;

public class AudioContentTest {
    private static final byte[] DATA = {1, 2, 3};

    @Test
    public void keepsDataAndMimeType() {
        InputAudioContent input = new InputAudioContent(DATA, "audio/wav");
        assertArrayEquals(DATA, input.getAudioData());
        assertEquals("audio/wav", input.getMimeType());

        OutputAudioContent output = new OutputAudioContent(DATA, " audio/ogg; codecs=opus ");
        assertArrayEquals(DATA, output.getAudioData());
        assertEquals("audio/ogg; codecs=opus", output.getMimeType());
    }

    @Test
    public void rejectsNonAudioMimeTypes() {
        assertThrows(IllegalArgumentException.class, () -> new InputAudioContent(DATA, null));
        assertThrows(IllegalArgumentException.class, () -> new InputAudioContent(DATA, "text/html"));
        assertThrows(IllegalArgumentException.class, () -> new OutputAudioContent(DATA, ""));
        assertThrows(IllegalArgumentException.class, () -> new OutputAudioContent(DATA, "video/mp4"));
    }
}
