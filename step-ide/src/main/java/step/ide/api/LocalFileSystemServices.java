package step.ide.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.ControllerServiceException;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// TODO SED-4429 Remove this service (probably)
@jakarta.ws.rs.Path("/local/fs")
@Tag(name = "Filesystem")
public class LocalFileSystemServices extends AbstractStepServices {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileSystemServices.class);

    // package-private and mutable for unit tests
    Path home = Paths.get(System.getProperty("user.home"));

    public record DirectoryListing(
        String currentPath,
        String parentPath,
        List<FileDescriptor> items
    ) {
    }

    public record FileDescriptor(
        String name,
        String absolutePath,
        long size,
        boolean isDirectory,
        boolean isFile,
        boolean isSymlink,
        boolean isHidden
    ) {
        public static FileDescriptor fromPath(Path path) {
            try {
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

                return new FileDescriptor(
                    path.getFileName() != null ? path.getFileName().toString() : path.toString(),
                    path.toAbsolutePath().toString(),
                    attrs.size(),
                    attrs.isDirectory(),
                    attrs.isRegularFile(),
                    Files.isSymbolicLink(path),
                    Files.isHidden(path)
                );
            } catch (IOException e) {
                // This could be broken symlinks etc., so don't spam the log with warnings
                logger.debug("Failed to create a file descriptor for path: {}", path, e);
                return null;
            }
        }
    }

    @GET
    @jakarta.ws.rs.Path("listDirectory")
    @Produces(MediaType.APPLICATION_JSON)
    public DirectoryListing listDirectory(
        @QueryParam("path") String pathString,
        @QueryParam("showHidden") @DefaultValue("false") boolean showHidden,
        @QueryParam("filesOnly") @DefaultValue("false") boolean filesOnly,
        @QueryParam("dirsOnly") @DefaultValue("false") boolean dirsOnly) {

        if (filesOnly && dirsOnly) {
            throw new ControllerServiceException(Response.Status.BAD_REQUEST.getStatusCode(), "Cannot specify both filesOnly and dirsOnly");
        }

        Path targetDir;
        try {
            targetDir = ((pathString != null && !pathString.isBlank()) ? Paths.get(pathString) : home).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new ControllerServiceException(Response.Status.BAD_REQUEST.getStatusCode(), "Invalid path: " + pathString);
        }

        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            throw new ControllerServiceException(Response.Status.BAD_REQUEST.getStatusCode(), "Path is not a valid directory: " + targetDir);
        }

        List<FileDescriptor> items = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir)) {
            for (Path entry : stream) {
                FileDescriptor descriptor = FileDescriptor.fromPath(entry);
                if (descriptor != null) {

                    // Apply query parameter filters
                    if (!showHidden && descriptor.isHidden()) {
                        continue;
                    }
                    if (filesOnly && !descriptor.isFile()) {
                        continue;
                    }
                    if (dirsOnly && !descriptor.isDirectory()) {
                        continue;
                    }

                    items.add(descriptor);
                }
            }
        } catch (AccessDeniedException e) {
            throw new ControllerServiceException(Response.Status.FORBIDDEN.getStatusCode(), "Permission denied to read directory: " + targetDir);
        } catch (IOException e) {
            throw new ControllerServiceException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Failed to read directory: " + e.getMessage(), e);
        }

        // Locale-sensitive collator (note: not thread-safe, but cheap to construct, so no need for a field)
        Collator collator = Collator.getInstance();
        // SECONDARY strength ignores case differences (a = A) but respects accents and umlauts (a < ä)
        collator.setStrength(Collator.SECONDARY);

        items.sort(Comparator
            .comparing(FileDescriptor::isDirectory).reversed()
            .thenComparing(FileDescriptor::name, collator));

        Path parent = targetDir.getParent();
        String parentPathStr = (parent != null) ? parent.toAbsolutePath().toString() : null;

        return new DirectoryListing(
            targetDir.toString(),
            parentPathStr,
            items
        );
    }

    @GET
    @jakarta.ws.rs.Path("roots")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FileDescriptor> getRoots() {
        List<FileDescriptor> roots = new ArrayList<>();
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            FileDescriptor descriptor = FileDescriptor.fromPath(root);
            if (descriptor != null) {
                roots.add(descriptor);
            }
        }
        return roots;
    }

    public record CreateDirectoryRequest(
        String parentPath,
        String name
    ) {
    }

    @jakarta.ws.rs.POST
    @jakarta.ws.rs.Path("createDirectory")
    @jakarta.ws.rs.Consumes(MediaType.APPLICATION_JSON)
    @jakarta.ws.rs.Produces(MediaType.APPLICATION_JSON)
    public FileDescriptor createDirectory(CreateDirectoryRequest request) {
        if (request == null || request.parentPath() == null || request.name() == null || request.name().isBlank()) {
            throw new ControllerServiceException(Response.Status.BAD_REQUEST.getStatusCode(), "Parent path and directory name are required");
        }

        // 1. Security: Strictly forbid path traversal characters in the new folder name
        String name = request.name().trim();
        if (name.contains("/") || name.contains("\\") || name.equals("..") || name.equals(".")) {
            throw new ControllerServiceException(Response.Status.BAD_REQUEST.getStatusCode(), "Directory name contains invalid characters");
        }

        // 2. Resolve and normalize the parent path
        Path parent = Paths.get(request.parentPath()).toAbsolutePath().normalize();

        if (!Files.exists(parent) || !Files.isDirectory(parent)) {
            throw new ControllerServiceException(Response.Status.BAD_REQUEST.getStatusCode(), "Parent path is not a valid directory: " + parent);
        }

        // 3. Construct the target path
        Path targetDir = parent.resolve(name).toAbsolutePath().normalize();

        // 4. Double-check that the resolution didn't escape the parent (Paranoia check, shouldn't be possible)
        if (!targetDir.getParent().equals(parent)) {
            throw new ControllerServiceException(Response.Status.BAD_REQUEST.getStatusCode(), "Path traversal detected");
        }

        // 5. Edge Case: Target already exists
        if (Files.exists(targetDir)) {
            throw new ControllerServiceException(Response.Status.CONFLICT.getStatusCode(), "A file or directory with this name already exists");
        }

        // 6. Execution
        try {
            Files.createDirectory(targetDir);

            FileDescriptor descriptor = FileDescriptor.fromPath(targetDir);
            if (descriptor == null) {
                // Totally unexpected situation, should not normally happen
                String msg = String.format("Created directory %s, but failed to create associated directory descriptor.", targetDir);
                logger.error(msg);
                throw new IOException(msg);
            }
            return descriptor;
        } catch (AccessDeniedException e) {
            logger.warn("Access denied while creating directory: {}", targetDir, e);
            throw new ControllerServiceException(Response.Status.FORBIDDEN.getStatusCode(), "Permission denied to create directory");
        } catch (IOException e) {
            logger.error("Failed to create directory: {}", targetDir, e);
            throw new ControllerServiceException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Failed to create directory: " + e.getMessage());
        }
    }
}
