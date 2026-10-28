package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputAudioContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputVideoContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputAudioContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputFileContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputVideoContent;
import dev.ohmvir.plugins.jenkinsaisynapse.utils.FileDownloadUtils;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kohsuke.stapler.StaplerResponse2;

public class FileDownloadTest {
    private static final byte[] DATA = {1, 2, 3};

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

    @Test
    public void outputFileIsServedAsOpaqueAttachment() throws Exception {
        FakeResponse rsp = new FakeResponse();
        new OutputFileContent("x\"; y=\"page.html", DATA, "text/html").doDownload(null, rsp.proxy());

        assertEquals("application/octet-stream", rsp.contentType);
        assertEquals("nosniff", rsp.headers.get("X-Content-Type-Options"));
        assertEquals("sandbox; default-src 'none'", rsp.headers.get("Content-Security-Policy"));
        assertEquals("attachment; filename=\"x___y__page.html\"", rsp.headers.get("Content-Disposition"));
        assertArrayEquals(DATA, rsp.body.toByteArray());
    }

    @Test
    public void inlineMediaIsServedWithoutSniffingInASandbox() throws Exception {
        FakeResponse inputAudio = new FakeResponse();
        new InputAudioContent(DATA, "audio/wav").doAudio(null, inputAudio.proxy());
        assertInlineMedia(inputAudio, "audio/wav");

        FakeResponse outputAudio = new FakeResponse();
        new OutputAudioContent(DATA, "audio/ogg; codecs=opus").doAudio(null, outputAudio.proxy());
        assertInlineMedia(outputAudio, "audio/ogg; codecs=opus");

        FakeResponse inputVideo = new FakeResponse();
        new InputVideoContent(DATA, "video/mp4").doVideo(null, inputVideo.proxy());
        assertInlineMedia(inputVideo, "video/mp4");

        FakeResponse outputVideo = new FakeResponse();
        new OutputVideoContent(DATA, "video/webm").doVideo(null, outputVideo.proxy());
        assertInlineMedia(outputVideo, "video/webm");
    }

    @Test
    public void emptyContentIsNotFound() throws Exception {
        FakeResponse file = new FakeResponse();
        new OutputFileContent("a.txt", new byte[0], "text/plain").doDownload(null, file.proxy());
        assertEquals(HttpServletResponse.SC_NOT_FOUND, file.error);

        FakeResponse audio = new FakeResponse();
        new OutputAudioContent(null, "audio/wav").doAudio(null, audio.proxy());
        assertEquals(HttpServletResponse.SC_NOT_FOUND, audio.error);
        assertNull(audio.contentType);
    }

    private static void assertInlineMedia(FakeResponse rsp, String mimeType) {
        assertEquals(mimeType, rsp.contentType);
        assertEquals("nosniff", rsp.headers.get("X-Content-Type-Options"));
        assertEquals("sandbox; default-src 'none'", rsp.headers.get("Content-Security-Policy"));
        assertNull(rsp.headers.get("Content-Disposition"));
        assertEquals(DATA.length, rsp.contentLength);
        assertArrayEquals(DATA, rsp.body.toByteArray());
    }

    /** Records what is written to a {@link StaplerResponse2}. */
    private static class FakeResponse {
        final Map<String, String> headers = new HashMap<>();
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        String contentType;
        int contentLength = -1;
        int error;

        StaplerResponse2 proxy() {
            ServletOutputStream out = new ServletOutputStream() {
                @Override
                public void write(int b) {
                    body.write(b);
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {}
            };
            return (StaplerResponse2) Proxy.newProxyInstance(
                    StaplerResponse2.class.getClassLoader(), new Class<?>[] {StaplerResponse2.class}, (p, m, args) -> {
                        switch (m.getName()) {
                            case "setContentType" -> contentType = (String) args[0];
                            case "setContentLength" -> contentLength = (int) args[0];
                            case "setHeader" -> headers.put((String) args[0], (String) args[1]);
                            case "sendError" -> error = (int) args[0];
                            case "getOutputStream" -> {
                                return out;
                            }
                            default -> throw new UnsupportedOperationException(m.getName());
                        }
                        return null;
                    });
        }
    }

    @Test
    public void usesTheGivenDefaultName() {
        assertEquals("image-1.png", FileDownloadUtils.sanitizeFileName(null, "image-1.png"));
        assertEquals("image-1.png", FileDownloadUtils.sanitizeFileName("...", "image-1.png"));
        assertEquals("a.txt", FileDownloadUtils.sanitizeFileName("a.txt", "image-1.png"));
    }

    @Test
    public void keepsTheEndOfLongNamesWithoutLeadingDots() {
        String name = FileDownloadUtils.sanitizeFileName("a".repeat(500) + ".txt");
        assertEquals(128, name.length());
        assertEquals(".txt", name.substring(name.length() - 4));
        assertEquals(
                "b".repeat(127),
                FileDownloadUtils.sanitizeFileName("a".repeat(500) + "." + "b".repeat(127)));
    }
}
