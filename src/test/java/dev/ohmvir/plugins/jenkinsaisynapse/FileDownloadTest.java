package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.ohmvir.plugins.jenkinsaisynapse.utils.FileDownloadUtils;
import org.junit.jupiter.api.Test;

public class FileDownloadTest {

    @Test
    public void keepsPlainFileNames() {
        assertEquals("report-v1.2_final.pdf", FileDownloadUtils.sanitizeFileName("report-v1.2_final.pdf"));
    }

    @Test
    public void stripsHeaderInjection() {
        assertEquals("a.html___X-Injected__1", FileDownloadUtils.sanitizeFileName("a.html\"\r\nX-Injected: 1"));
    }

    @Test
    public void stripsPathsAndLeadingDots() {
        assertEquals("passwd", FileDownloadUtils.sanitizeFileName("../../etc/passwd"));
        assertEquals("evil.exe", FileDownloadUtils.sanitizeFileName("C:\\Windows\\evil.exe"));
        assertEquals("bashrc", FileDownloadUtils.sanitizeFileName(".bashrc"));
    }

    @Test
    public void fallsBackForEmptyNames() {
        assertEquals("download", FileDownloadUtils.sanitizeFileName(null));
        assertEquals("download", FileDownloadUtils.sanitizeFileName(""));
        assertEquals("download", FileDownloadUtils.sanitizeFileName("dir/"));
        assertEquals("download", FileDownloadUtils.sanitizeFileName("..."));
    }

    @Test
    public void boundsLength() {
        assertEquals(
                128,
                FileDownloadUtils.sanitizeFileName("a".repeat(500) + ".txt").length());
    }
}
