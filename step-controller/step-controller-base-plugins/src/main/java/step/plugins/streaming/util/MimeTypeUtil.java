package step.plugins.streaming.util;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MimeTypeUtil {
    private MimeTypeUtil() {
    }

    private static final Map<String, String> mimeTypeForExtension = makeMap();

    private static Map<String, String> makeMap() {
        Map<String, String> map = new HashMap<>();

        // Text & Code
        List.of("txt", "log", "ini", "conf", "java", "c", "cpp", "h", "hpp", "sh", "bat", "ps1", "sql", "php", "rb", "pl", "go", "rs")
            .forEach(m -> map.put(m, "text/plain"));
        List.of("md", "markdown").forEach(m -> map.put(m, "text/markdown"));
        map.put("csv", "text/csv");
        List.of("htm", "html").forEach(m -> map.put(m, "text/html"));

        // Application Data
        map.put("json", "application/json");
        map.put("xml", "application/xml");
        List.of("yaml", "yml").forEach(m -> map.put(m, "application/yaml"));
        map.put("pdf", "application/pdf");

        // Images
        List.of("jpg", "jpeg").forEach(m -> map.put(m, "image/jpeg"));
        map.put("png", "image/png");
        map.put("gif", "image/gif");
        map.put("bmp", "image/bmp");
        map.put("webp", "image/webp");
        List.of("tiff", "tif").forEach(m -> map.put(m, "image/tiff"));
        map.put("svg", "image/svg+xml");
        map.put("heic", "image/heic");
        map.put("avif", "image/avif");

        // Video
        List.of("mp4", "m4v").forEach(m -> map.put(m, "video/mp4"));
        map.put("mkv", "video/x-matroska");
        map.put("mov", "video/quicktime");
        map.put("avi", "video/x-msvideo");
        map.put("wmv", "video/x-ms-vwm");
        map.put("flw", "video/x-flv");
        map.put("webm", "video/webm");
        map.put("mpeg", "video/mpeg");
        map.put("mpg", "video/mpg");
        map.put("3gp", "video/3gpp");
        map.put("3g2", "video/3gpp2");
        map.put("ogv", "video/ogg");
        return map;
    }

    public static String getMimeTypeForFilename(String filename) {
        if (filename == null) {
            // unexpected, but let's be prudent
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot == -1) {
            // no extension
            return null;
        }
        String extension = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return mimeTypeForExtension.get(extension);
    }
}
