package dev.ohmvir.plugins.jenkinsaisynapse.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputFileContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputImageContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import hudson.AbortException;
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
        assertEquals("passwd", unique("../../etc/passwd", new HashSet<>()));
        assertEquals("passwd", unique("..\\..\\passwd", new HashSet<>()));
        assertEquals("a_b.png", unique("a b.png", new HashSet<>()));
    }

    @Test
    public void emptyOutputFileNamesFallBackToTheGeneratedName() {
        assertEquals("image-2.png", WorkspaceFiles.uniqueFileName("..", "image-2.png", 2, new HashSet<>()));
        assertEquals("image-3.png", WorkspaceFiles.uniqueFileName(null, "image-3.png", 3, new HashSet<>()));
        assertEquals("file-4.txt", WorkspaceFiles.uniqueFileName("", "file-4.txt", 4, new HashSet<>()));
        assertEquals("file-5.txt", WorkspaceFiles.uniqueFileName("dir/", "file-5.txt", 5, new HashSet<>()));
    }

    @Test
    public void longOutputFileNamesAreBoundedAndKeepTheirExtension() {
        String fileName = unique("a".repeat(500) + ".png", new HashSet<>());
        assertEquals(128, fileName.length());
        assertTrue(fileName.endsWith(".png"));
        Set<String> used = new HashSet<>();
        unique("a".repeat(500), used);
        assertTrue(unique("a".repeat(500), used).length() <= 140);
    }

    @Test
    public void windowsDeviceNamesAreNotUsedAsOutputFileNames() {
        assertEquals("file-CON", unique("CON", new HashSet<>()));
        assertEquals("file-NUL.txt", unique("NUL.txt", new HashSet<>()));
        assertEquals("file-com1.png", unique("com1.png", new HashSet<>()));
        assertEquals("file-Lpt9", unique("Lpt9", new HashSet<>()));
        assertEquals("file-aux.tar.gz", unique("dir/aux.tar.gz", new HashSet<>()));
        assertEquals("console.txt", unique("console.txt", new HashSet<>()));
        assertEquals("nul_1.txt", unique("nul 1.txt", new HashSet<>()));
        assertEquals("com0", unique("com0", new HashSet<>()));
    }

    @Test
    public void outputFileNamesDoNotOverwriteEachOther() {
        Set<String> used = new HashSet<>();
        assertEquals("a.png", unique("a.png", used));
        assertEquals("2-a.png", WorkspaceFiles.uniqueFileName("a.png", "image-2.png", 2, used));
    }

    private static String unique(String name, Set<String> used) {
        return WorkspaceFiles.uniqueFileName(name, "output.bin", 1, used);
    }

    @Test
    public void contentTypeIsGuessedFromTheNameThenTheData() {
        byte[] text = "hello".getBytes(StandardCharsets.UTF_8);
        assertEquals("image/png", WorkspaceFiles.guessContentType("shot.PNG", text));
        assertEquals("video/mp4", WorkspaceFiles.guessContentType("clip.mp4", text));
        assertEquals("text/markdown", WorkspaceFiles.guessContentType("README.md", text));
        assertEquals("text/plain", WorkspaceFiles.guessContentType("LICENSE", text));
        assertEquals("text/plain", WorkspaceFiles.guessContentType("notes.txt", text));
        assertEquals("application/yaml", WorkspaceFiles.guessContentType("ci.yml", text));
        assertEquals("image/svg+xml", WorkspaceFiles.guessContentType("logo.svg", text));
        assertEquals("application/octet-stream", WorkspaceFiles.guessContentType("blob", new byte[] {1, 0, 2}));
    }

    @Test
    public void onlySupportedRasterImagesAreAttachedAsImages(@TempDir File dir) throws Exception {
        for (String name : new String[] {"a.png", "a.jpg", "a.jpeg", "a.gif", "a.webp"}) {
            Files.write(new File(dir, name).toPath(), new byte[] {1, 2, 3});
            assertInstanceOf(InputImageContent.class, WorkspaceFiles.readAttachment(new FilePath(dir), name));
        }
        for (String name : new String[] {"a.bmp", "a.tif"}) {
            Files.write(new File(dir, name).toPath(), new byte[] {1, 0, 2});
            assertInstanceOf(InputFileContent.class, WorkspaceFiles.readAttachment(new FilePath(dir), name));
        }
    }

    @Test
    public void missingOrBlankAttachmentPathsAreRejectedWithAMessage(@TempDir File dir) {
        for (String path : new String[] {null, "", "  "}) {
            AbortException e = assertThrows(
                    AbortException.class, () -> WorkspaceFiles.readAttachment(new FilePath(dir), path));
            assertTrue(e.getMessage().contains("has no path"), e.getMessage());
        }
    }

    @Test
    public void missingFilesAndDirectoriesAreRejectedWithAMessage(@TempDir File dir) throws Exception {
        assertTrue(new File(dir, "sub").mkdir());
        AbortException missing = assertThrows(
                AbortException.class, () -> WorkspaceFiles.readAttachment(new FilePath(dir), "nope.txt"));
        assertTrue(missing.getMessage().contains("does not exist: nope.txt"), missing.getMessage());
        AbortException directory =
                assertThrows(AbortException.class, () -> WorkspaceFiles.readAttachment(new FilePath(dir), "sub"));
        assertTrue(directory.getMessage().contains("is a directory: sub"), directory.getMessage());
    }

    @Test
    public void textFilesAreAttachedAsTextAndBinaryDocumentsAsFiles(@TempDir File dir) throws Exception {
        for (String name : new String[] {"a.md", "a.txt", "a.json", "a.yaml", "a.yml", "a.csv", "a.xml", "a.svg"}) {
            Files.writeString(new File(dir, name).toPath(), "content of " + name);
            InputTextContent text =
                    assertInstanceOf(InputTextContent.class, WorkspaceFiles.readAttachment(new FilePath(dir), name));
            assertTrue(text.getText().startsWith("File " + name + ":\n"), text.getText());
            assertTrue(text.getText().endsWith("content of " + name), text.getText());
        }
        Files.write(new File(dir, "a.pdf").toPath(), new byte[] {'%', 'P', 'D', 'F'});
        assertInstanceOf(InputFileContent.class, WorkspaceFiles.readAttachment(new FilePath(dir), "a.pdf"));
    }
}
