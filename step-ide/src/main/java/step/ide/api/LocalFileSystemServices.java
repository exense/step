package step.ide.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.deployment.AbstractStepServices;
import step.core.filebrowser.DirectoryListing;
import step.core.filebrowser.FileDescriptor;
import step.core.filebrowser.FileDescriptors;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

@jakarta.ws.rs.Path("/local/fs")
@Tag(name = "Filesystem")
public class LocalFileSystemServices extends AbstractStepServices {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileSystemServices.class);

    // package-private and mutable for unit tests
    Path home = Paths.get(System.getProperty("user.home"));

    /**
     * Describes a path as a {@link FileDescriptor}, the payload shared with the automation package
     * browser. In this namespace the identity of an entry and the value stored when it is picked are
     * both its absolute path.
     *
     * @return {@code null} if the attributes cannot be read, e.g. for a broken symlink
     */
    private static FileDescriptor toFileDescriptor(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            String absolutePath = path.toAbsolutePath().toString();

            return new FileDescriptor(
                path.getFileName() != null ? path.getFileName().toString() : path.toString(),
                absolutePath,
                attrs.isDirectory(),
                attrs.isRegularFile(),
                Files.isHidden(path),
                Files.isSymbolicLink(path),
                // the size a file system reports for a directory is an implementation detail (0, 4096,
                // ...) that means nothing to the client, so it is reported as 'not applicable'
                attrs.isDirectory() ? null : attrs.size(),
                absolutePath
            );
        } catch (IOException e) {
            // This could be broken symlinks etc., so don't spam the log with warnings
            logger.debug("Failed to create a file descriptor for path: {}", path, e);
            return null;
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
            throw new WebApplicationException("Cannot specify both filesOnly and dirsOnly", Response.Status.BAD_REQUEST);
        }

        Path targetDir;
        try {
            targetDir = ((pathString != null && !pathString.isBlank()) ? Paths.get(pathString) : home).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new WebApplicationException("Invalid path: " + pathString, Response.Status.BAD_REQUEST);
        }

        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            throw new WebApplicationException("Path is not a valid directory: " + targetDir, Response.Status.BAD_REQUEST);
        }

        List<FileDescriptor> items = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir)) {
            for (Path entry : stream) {
                FileDescriptor descriptor = toFileDescriptor(entry);
                if (descriptor != null) {

                    // Apply query parameter filters
                    if (!showHidden && descriptor.hidden()) {
                        continue;
                    }
                    if (filesOnly && !descriptor.regularFile()) {
                        continue;
                    }
                    if (dirsOnly && !descriptor.directory()) {
                        continue;
                    }

                    items.add(descriptor);
                }
            }
        } catch (AccessDeniedException e) {
            throw new WebApplicationException("Permission denied to read directory: " + targetDir, Response.Status.FORBIDDEN);
        } catch (IOException e) {
            throw new WebApplicationException("Failed to read directory: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }

        items.sort(FileDescriptors.byDirectoryThenName());

        Path parent = targetDir.getParent();
        String parentPathStr = (parent != null) ? parent.toAbsolutePath().toString() : null;

        return new DirectoryListing(
            targetDir.toString(),
            parentPathStr,
            targetDir.toString(),
            items
        );
    }

    @GET
    @jakarta.ws.rs.Path("roots")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FileDescriptor> getRoots() {
        List<FileDescriptor> roots = new ArrayList<>();
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            FileDescriptor descriptor = toFileDescriptor(root);
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
            throw new WebApplicationException("Parent path and directory name are required", Response.Status.BAD_REQUEST);
        }

        // 1. Security: Strictly forbid path traversal characters in the new directory name
        String name = request.name().trim();
        if (name.contains("/") || name.contains("\\") || name.equals("..") || name.equals(".")) {
            throw new WebApplicationException("Directory name contains invalid characters", Response.Status.BAD_REQUEST);
        }

        // 2. Resolve and normalize the parent path
        Path parent = Paths.get(request.parentPath()).toAbsolutePath().normalize();

        if (!Files.exists(parent) || !Files.isDirectory(parent)) {
            throw new WebApplicationException("Parent path is not a valid directory: " + parent, Response.Status.BAD_REQUEST);
        }

        // 3. Construct the target path
        Path targetDir = parent.resolve(name).toAbsolutePath().normalize();

        // 4. Double-check that the resolution didn't escape the parent (Paranoia check, shouldn't be possible)
        if (!targetDir.getParent().equals(parent)) {
            throw new WebApplicationException("Path traversal detected", Response.Status.BAD_REQUEST);
        }

        // 5. Edge Case: Target already exists
        if (Files.exists(targetDir)) {
            throw new WebApplicationException("A file or directory with this name already exists", Response.Status.CONFLICT);
        }

        // 6. Execution
        try {
            Files.createDirectory(targetDir);

            FileDescriptor descriptor = toFileDescriptor(targetDir);
            if (descriptor == null) {
                // Totally unexpected situation, should not normally happen
                String msg = String.format("Created directory %s, but failed to create associated directory descriptor.", targetDir);
                logger.error(msg);
                throw new IOException(msg);
            }
            return descriptor;
        } catch (AccessDeniedException e) {
            logger.warn("Access denied while creating directory: {}", targetDir, e);
            throw new WebApplicationException("Permission denied to create directory", Response.Status.FORBIDDEN);
        } catch (IOException e) {
            logger.error("Failed to create directory: {}", targetDir, e);
            throw new WebApplicationException("Failed to create directory: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
