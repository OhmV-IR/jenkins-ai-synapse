package dev.ohmvir.plugins.jenkinsaisynapse.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import java.util.Locale;
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
        assertTrue(unique("a".repeat(500), used).length() <= 128);
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

    @Test
    public void collisionNamesAreNotUsedByAnotherOutput() {
        Set<String> used = new HashSet<>();
        assertEquals("3-a.png", uniqueAt("3-a.png", 1, used));
        assertEquals("a.png", uniqueAt("a.png", 2, used));
        // 3-a.png is taken by the first output, so the third must not overwrite it
        assertEquals("3-2-a.png", uniqueAt("a.png", 3, used));
    }

    @Test
    public void outputFileNamesDifferingOnlyInCaseDoNotOverwriteEachOther() {
        Set<String> used = new HashSet<>();
        assertEquals("Report.pdf", uniqueAt("Report.pdf", 1, used));
        assertEquals("2-report.pdf", uniqueAt("report.pdf", 2, used));
        assertEquals("3-REPORT.PDF", uniqueAt("REPORT.PDF", 3, used));
        // A name that only differs in case from an earlier collision name is a collision as well
        assertEquals("4-Report.pdf", uniqueAt("Report.pdf", 4, used));
        assertEquals("5-2-REPORT.pdf", uniqueAt("2-REPORT.pdf", 5, used));
    }

    @Test
    public void manyOutputsWithTheSameNameAllGetDistinctNames() {
        Set<String> used = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            // The same index each time, as the index is only a hint
            names.add(uniqueAt("a.png", 1, used).toLowerCase(Locale.ROOT));
        }
        assertEquals(50, names.size());
    }

    @Test
    public void collisionNamesStayWithinTheLengthLimitAndKeepTheirExtension() {
        Set<String> used = new HashSet<>();
        Set<String> names = new HashSet<>();
        String longName = "a".repeat(500) + ".png";
        for (int i = 0; i < 20; i++) {
            String fileName = uniqueAt(longName, 7, used);
            assertTrue(fileName.length() <= 128, fileName);
            assertTrue(fileName.endsWith(".png"), fileName);
            names.add(fileName);
        }
        assertEquals(20, names.size());
        // An extension too long to keep next to the prefix is shortened rather than exceeding the limit
        String longExtension = "a." + "b".repeat(126);
        used = new HashSet<>();
        String first = uniqueAt(longExtension, 1, used);
        String second = uniqueAt(longExtension, 1, used);
        assertEquals(128, first.length());
        assertEquals(128, second.length());
        assertNotEquals(first, second);
    }

    @Test
    public void extensionlessOutputFileNamesGetTheExtensionOfTheirContentType() {
        assertEquals("file_011CN.pdf", withExtension("file_011CN", "pdf"));
        assertEquals("notes.txt", withExtension("notes", "txt"));
        // A name that is not usable and falls back to the generated name already has an extension
        assertEquals("output.bin", withExtension("..", "pdf"));
        // A trailing dot is not an extension, neither is a version number
        assertEquals("bashrc.pdf", withExtension(".bashrc", "pdf"));
        assertEquals("notes.pdf", withExtension("notes.", "pdf"));
        assertEquals("model-1.5.pdf", withExtension("model-1.5", "pdf"));
        assertEquals("v1.2.3.pdf", withExtension("v1.2.3", "pdf"));
        // A name that is reserved on Windows is still avoided once it has an extension
        assertEquals("file-CON.pdf", withExtension("CON", "pdf"));
    }

    @Test
    public void outputFileNamesWithAnExtensionAreNotChanged() {
        assertEquals("report.pdf", withExtension("report.pdf", "pdf"));
        assertEquals("report.docx", withExtension("report.docx", "pdf"));
        assertEquals("Report.PDF", withExtension("Report.PDF", "pdf"));
        assertEquals("archive.tar.gz", withExtension("archive.tar.gz", "bin"));
        assertEquals("a.7z", withExtension("a.7z", "bin"));
    }

    @Test
    public void outputFileNamesWithAnUnknownContentTypeGetTheUnknownExtension() {
        // "bin" is what extensionFor returns for a content type it does not know, as for outputs without a name
        assertEquals("file_011CN.bin", withExtension("file_011CN", "bin"));
        // Without an extension to add, the name is kept as it is
        assertEquals(
                "file_011CN", WorkspaceFiles.uniqueFileName("file_011CN", "output.bin", null, 1, new HashSet<>()));
    }

    @Test
    public void outputFileNamesStayBoundedWhenAnExtensionIsAdded() {
        String fileName = withExtension("a".repeat(500), "pdf");
        assertEquals(128, fileName.length());
        assertEquals("a".repeat(124) + ".pdf", fileName);
        // The name is cut before the extension, not after it, and does not end in a dot
        String dotted = withExtension("a".repeat(123) + "." + "b".repeat(30) + "-", "pdf");
        assertTrue(dotted.length() <= 128, dotted);
        assertTrue(dotted.endsWith(".pdf"), dotted);
        assertTrue(!dotted.contains(".."), dotted);
        // A duplicate of a long extensionless name is still bounded, and keeps the added extension
        Set<String> used = new HashSet<>();
        withExtension("a".repeat(500), used);
        String duplicate = withExtension("a".repeat(500), used);
        assertEquals(128, duplicate.length());
        assertTrue(duplicate.endsWith(".pdf"), duplicate);
    }

    @Test
    public void extensionlessOutputFilesWithTheSameNameDoNotOverwriteEachOther() {
        Set<String> used = new HashSet<>();
        assertEquals("a.pdf", withExtension("a", used));
        assertEquals("2-A.pdf", WorkspaceFiles.uniqueFileName("A", "output.bin", "pdf", 2, used));
    }

    private static String withExtension(String name, String extension) {
        return WorkspaceFiles.uniqueFileName(name, "output.bin", extension, 1, new HashSet<>());
    }

    private static String withExtension(String name, Set<String> used) {
        return WorkspaceFiles.uniqueFileName(name, "output.bin", "pdf", 1, used);
    }

    private static String uniqueAt(String name, int index, Set<String> used) {
        return WorkspaceFiles.uniqueFileName(name, "output.bin", index, used);
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
