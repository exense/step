package step.ide.exceptions;

import java.nio.file.Path;
import java.util.Objects;

// Exception to signal that a particular path (file/directory) already exists, when it shouldn't.
public class FileExistsException extends Exception {
    public final Path existingPath;

    public FileExistsException(Path existingPath) {
        this.existingPath = Objects.requireNonNull(existingPath).toAbsolutePath().normalize();
    }
}
