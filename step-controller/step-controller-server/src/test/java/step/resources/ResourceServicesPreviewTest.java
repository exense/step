package step.resources;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static step.resources.ResourceServices.getPreviewText;

public class ResourceServicesPreviewTest {

    // This class only tests the "preview" functionality of the ResourceServices class (which is a static method not requiring any context or other setup).

    private InputStream createStream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testReadsUntilEof() throws Exception {
        String input = "Line 1\nLine 2";
        InputStream is = createStream(input);

        String result = getPreviewText(is, 10, 100);

        assertEquals("Should read the entire stream", input, result);
    }

    @Test
    public void testStopsAtLineLimit() throws Exception {
        String input = "Line 1\nLine 2\nLine 3\nLine 4";
        InputStream is = createStream(input);

        String result = getPreviewText(is, 2, 100);

        assertEquals("Should stop exactly after the second newline", "Line 1\nLine 2\n", result);
    }

    @Test
    public void testStopsAtByteLimit() throws Exception {
        String input = "123456789\nNext Line";
        InputStream is = createStream(input);

        String result = getPreviewText(is, 5, 11);

        assertEquals("Should stop due to byte limit", "123456789\nN", result);
    }

    @Test
    public void testEmptyStream() throws Exception {
        InputStream is = createStream("");

        String result = getPreviewText(is, 10, 100);

        assertEquals("Should handle empty streams gracefully", "", result);
    }

    @Test
    public void testBinaryDataWithAccidentalNewline() throws Exception {
        // Simulating a binary file with non-printable/non-UTF8 bytes and a newline (0x0A)
        byte[] binaryData = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A,
            0x1A, 0x0A,
            (byte) 0xFF, (byte) 0xFF
        };
        InputStream is = new ByteArrayInputStream(binaryData);

        // Limit to 2 lines, which will process the first 8 bytes
        String result = getPreviewText(is, 2, 100);

        // The reading loop copies bytes exactly. The String constructor handles invalid
        // UTF-8 bytes (like 0x89) by silently inserting the Unicode Replacement Character.
        byte[] expectedBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        String expected = new String(expectedBytes, StandardCharsets.UTF_8);

        assertEquals("Should decode invalid UTF-8 gracefully without throwing exceptions", expected, result);
    }

    @Test
    public void testStopsAtByteLimit_PureBinaryNoNewline() throws Exception {
        // A sequence of bytes with no 0x0A present
        byte[] pureBinary = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
        InputStream is = new ByteArrayInputStream(pureBinary);

        // Request 10 lines, but limit to 4 bytes
        String result = getPreviewText(is, 10, 4);

        // It should read exactly the first 4 bytes and stop
        byte[] expectedBytes = new byte[]{0x00, 0x01, 0x02, 0x03};
        String expected = new String(expectedBytes, StandardCharsets.UTF_8);

        assertEquals("Should cut off pure binary data exactly at the byte limit", expected, result);
    }

    @Test
    public void testZeroLimits() throws Exception {
        String input = "Line 1\nLine 2";

        InputStream is1 = createStream(input);
        assertEquals("Zero line limit should return empty", "", getPreviewText(is1, 0, 100));

        InputStream is2 = createStream(input);
        assertEquals("Zero byte limit should return empty", "", getPreviewText(is2, 10, 0));
    }
}
