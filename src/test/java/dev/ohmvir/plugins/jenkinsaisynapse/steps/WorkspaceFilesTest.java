package dev.ohmvir.plugins.jenkinsaisynapse.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputFileContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputImageContent;
import hudson.FilePath;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class WorkspaceFilesTest {
    @Test
    public void outputFileNamesStayInTheOutputDirectory() {
        assertEquals("passwd", WorkspaceFiles.uniqueFileName("../../etc/passwd", 1, new HashSet<>()));
        assertEquals("passwd", WorkspaceFiles.uniqueFileName("..\\..\\passwd", 1, new HashSet<>()));
        assertEquals("output-2", WorkspaceFiles.uniqueFileName("..", 2, new HashSet<>()));
        assertEquals("output-3", WorkspaceFiles.uniqueFileName(null, 3, new HashSet<>()));
        assertEquals("a_b.png", WorkspaceFiles.uniqueFileName("a b.png", 1, new HashSet<>()));
    }

    @Test
    public void outputFileNamesDoNotOverwriteEachOther() {
        Set<String> used = new HashSet<>();
        assertEquals("a.png", WorkspaceFiles.uniqueFileName("a.png", 1, used));
        assertEquals("2-a.png", WorkspaceFiles.uniqueFileName("a.png", 2, used));
    }

    @Test
    public void contentTypeIsGuessedFromTheNameThenTheData() {
        byte[] text = "hello".getBytes(StandardCharsets.UTF_8);
        assertEquals("image/png", WorkspaceFiles.guessContentType("shot.PNG", text));
        assertEquals("video/mp4", WorkspaceFiles.guessContentType("clip.mp4", text));
        assertEquals("text/markdown", WorkspaceFiles.guessContentType("README.md", text));
        assertEquals("text/plain", WorkspaceFiles.guessContentType("LICENSE", text));
        assertEquals("application/octet-stream", WorkspaceFiles.guessContentType("blob", new byte[] {1, 0, 2}));
    }

    @Test
    public void onlySupportedRasterImagesAreAttachedAsImages(@TempDir File dir) throws Exception {
        for (String name : new String[] {"a.png", "a.jpg", "a.jpeg", "a.gif", "a.webp"}) {
            Files.write(new File(dir, name).toPath(), new byte[] {1, 2, 3});
            assertInstanceOf(InputImageContent.class, WorkspaceFiles.readAttachment(new FilePath(dir), name));
        }
        for (String name : new String[] {"a.svg", "a.bmp", "a.tif"}) {
            Files.write(new File(dir, name).toPath(), new byte[] {1, 2, 3});
            assertInstanceOf(InputFileContent.class, WorkspaceFiles.readAttachment(new FilePath(dir), name));
        }
    }
}
