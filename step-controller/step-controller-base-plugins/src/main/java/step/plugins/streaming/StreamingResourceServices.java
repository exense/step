package step.plugins.streaming;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import step.core.GlobalContext;
import step.core.deployment.AbstractStepAsyncServices;
import step.framework.server.security.Secured;
import step.resources.ResourceMissingException;
import step.streaming.common.StreamingResourceTransferStatus;
import step.streaming.server.FilesystemStreamingResourcesStorageBackend;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/streaming-resources")
@Tag(name = "Streaming Resources")
public class StreamingResourceServices extends AbstractStepAsyncServices {

    StepStreamingResourceManager manager;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext globalContext = getContext();
        manager = globalContext.get(StepStreamingResourceManager.class);
    }

    @GET
    @Path("/demo")
    @Produces(MediaType.TEXT_HTML)
    // This is (probably) temporary, but I actually like it :-D
    public Response demo() {
        boolean devMode = false; // Set this to false in production

        try {
            String html;
            if (devMode) {
                // Load from file (absolute path or relative to project)
                java.nio.file.Path path = Paths.get("/home/cl/IdeaProjects/step-aio/step/step-controller/step-controller-base-plugins/src/main/resources/step/plugins/streaming/StreamingDemo.html");
                html = Files.readString(path);
            } else {
                // Load from resource (packaged in JAR / classpath)
                try (InputStream in = getClass().getResourceAsStream("StreamingDemo.html")) {
                    if (in == null) {
                        return Response.status(Response.Status.NOT_FOUND).entity("HTML resource not found.").build();
                    }
                    html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            return Response.ok(html, MediaType.TEXT_HTML).build();

        } catch (IOException e) {
            return Response.serverError().entity("Failed to load HTML: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/{id}/download")
    @Secured(right = "resource-read")
    public Response download(@PathParam("id") String resourceId, @QueryParam("start") Long start, @QueryParam("end") Long end, @QueryParam("inline") boolean inline) throws IOException {
        StreamingResource entity;
        try {
            entity = manager.getCatalog().getEntity(resourceId);
        } catch (Exception e) {
            throw new ResourceMissingException(resourceId);
        }

        manager.checkDownloadPermission(entity, getSession());

        long currentSize = Optional.ofNullable(entity.status.getCurrentSize()).orElse(0L);
        if (start != null && (start < 0 || start > currentSize)) {
            throw new IllegalArgumentException("start must be between 0 and " + currentSize);
        }
        if (end != null && (end < 0 || end > currentSize)) {
            throw new IllegalArgumentException("end must be between 0 and " + currentSize);
        }
        if (end != null && start != null && end < start) {
            throw new IllegalArgumentException("end must not be smaller than start");
        }

        long downloadStart = start == null ? 0 : start;
        long downloadEnd = end == null ? currentSize : end;


        StreamingOutput streamingOutput = outputStream -> {
            try (outputStream; InputStream inputStream = manager.getStorage().openReadStream(resourceId, downloadStart, downloadEnd)) {
                inputStream.transferTo(outputStream);
            } catch (IOException e) {
                throw new WebApplicationException(e);
            }
        };

        String mimeType = entity.mimeType;
        if (mimeType.startsWith("text/")) {
            mimeType += "; charset=UTF-8";
        }
        String utf8Filename = entity.filename;
        String asciiFilename = makeAsciiFilename(utf8Filename);
        String filenameHeaderFragment = "; filename=\"" + asciiFilename + "\"";
        if (!asciiFilename.equals(utf8Filename)) {
            filenameHeaderFragment += "; filename*=UTF-8''" + encodeUtf8Filename(utf8Filename);
        }

        return Response.ok(streamingOutput, mimeType)
                .header("Content-Disposition", (inline ? "inline" : "attachment") + filenameHeaderFragment)
                .build();
    }

    @GET
    @Path("/{id}/lines")
    @Secured(right = "resource-read")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getLines(@PathParam("id") String resourceId, @QueryParam("startIndex") long startIndex, @QueryParam("count") long count) throws IOException {
        StreamingResource entity;
        try {
            entity = manager.getCatalog().getEntity(resourceId);
        } catch (Exception e) {
            throw new ResourceMissingException(resourceId);
        }

        manager.checkDownloadPermission(entity, getSession());
        // argument validation is performed in this method, may throw various exceptions
        Stream<String> lines = manager.getLines(resourceId, startIndex, count);
        return lines.collect(Collectors.toList());
    }


    private static String makeAsciiFilename(String utf8Filename) {
        if (utf8Filename == null || utf8Filename.isEmpty()) {
            return "file"; // fallback default name
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < utf8Filename.length(); i++) {
            char c = utf8Filename.charAt(i);

            if (c <= 0x1F || c == 0x7F) {
                // Control characters
                sb.append('_');
            } else if (c > 0x7F) {
                // Non-ASCII
                sb.append('_');
            } else if ("\\/:*?\"<>|".indexOf(c) >= 0) {
                // Forbidden on most file systems
                sb.append('_');
            } else {
                sb.append(c);
            }
        }

        String result = sb.toString();

        // Remove trailing dots or spaces (Windows issue)
        result = result.replaceAll("[.\\s]+$", "");

        return result.isEmpty() ? "file" : result;
    }

    public static String encodeUtf8Filename(String filename) {
        StringBuilder sb = new StringBuilder();

        for (byte b : filename.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;

            // RFC 5987: "normal" unreserved characters
            if ((c >= 'a' && c <= 'z') ||
                    (c >= 'A' && c <= 'Z') ||
                    (c >= '0' && c <= '9') ||
                    c == '-' || c == '.' ||
                    c == '_' || c == '~') {
                sb.append((char) c);
            } else {
                // anything else: percent-encode
                sb.append('%');
                sb.append(String.format("%02X", c));
            }
        }

        return sb.toString();
    }

}
