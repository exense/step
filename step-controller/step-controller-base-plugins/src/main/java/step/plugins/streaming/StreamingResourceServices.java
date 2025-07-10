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
import java.util.Optional;

@Path("/streaming-resources")
@Tag(name = "Streaming Resources")
public class StreamingResourceServices extends AbstractStepAsyncServices {

    StreamingResourceCollectionCatalogBackend catalog;
    FilesystemStreamingResourcesStorageBackend storage;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext globalContext = getContext();
        catalog = globalContext.get(StreamingResourceCollectionCatalogBackend.class);
        storage = globalContext.get(FilesystemStreamingResourcesStorageBackend.class);
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
            entity = catalog.getEntity(resourceId);
        } catch (Exception e) {
            throw new ResourceMissingException(resourceId);
        }
        if (entity.status.getTransferStatus() == StreamingResourceTransferStatus.FAILED) {
            throw new IllegalArgumentException("The resource with ID " + resourceId + " has status " + entity.status.getTransferStatus());
        }
        long currentSize = Optional.ofNullable(entity.status.getCurrentSize()).orElse(0L);
        if (start != null && (start < 0 || start > currentSize)) {
            throw new IllegalArgumentException("start must be between 0 and " + (currentSize - 1));
        }
        if (end != null && (end < 0 || end > currentSize)) {
            throw new IllegalArgumentException("end must be between 0 and " + (currentSize - 1));
        }
        if (end != null && start != null && end < start) {
            throw new IllegalArgumentException("end must not be smaller than start");
        }

        long downloadStart = start == null ? 0 : start;
        long downloadEnd = end == null ? currentSize : end;


        StreamingOutput streamingOutput = outputStream -> {
            try (outputStream; InputStream inputStream = storage.openReadStream(resourceId, downloadStart, downloadEnd)) {
                inputStream.transferTo(outputStream);
            } catch (IOException e) {
                throw new WebApplicationException(e);
            }
        };

        return Response.ok(streamingOutput, entity.mimeType)
                .header("Content-Disposition", (inline ? "inline": "attachment") + "; filename=" + entity.filename)
                .build();
    }
}
