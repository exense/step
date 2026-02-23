package step.automation.packages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

/**
 * Utility class for creating a flat JAR archive that bundles all classpath entries
 * visible to a given {@link ClassLoader}.
 *
 * <p>The archive is produced by walking the classloader hierarchy (and, as a fallback,
 * the {@code java.class.path} system property) and merging every discovered classpath
 * entry — either a directory tree or an existing JAR file — into a single output JAR.
 * Entries are stored uncompressed ({@link ZipEntry#STORED}) for maximum throughput.
 * Duplicate entries (same relative path) are silently skipped so that the first
 * occurrence wins, which mirrors the standard classloader delegation model.</p>
 *
 * <p>The resulting archive always contains a minimal {@code META-INF/MANIFEST.MF}
 * (version {@code 1.0} only) and never re-exports the original manifests of merged
 * JARs.</p>
 */
public class ClassLoaderArchiver {

    private static final Logger logger = LoggerFactory.getLogger(ClassLoaderArchiver.class);

    private static final int BUFFER_SIZE = 64 * 1024; // 64KB

    /**
     * Creates a JAR archive at {@code outputFile} that contains all classes and
     * resources reachable from {@code classLoader}.
     *
     * <p>The method collects every classpath root (directories and JAR files) from
     * the classloader hierarchy via {@link #collectClasspathFiles(ClassLoader)} and
     * merges them into one flat JAR. The archive uses uncompressed ({@code STORED})
     * entries to minimise CPU overhead during creation.</p>
     *
     * @param classLoader the classloader whose full classpath should be archived;
     *                    its parent chain is traversed recursively
     * @param outputFile  the file to write the archive to; it is created or
     *                    overwritten if it already exists
     * @throws IOException if an I/O error occurs while reading classpath entries
     *                     or writing the output file
     */
    public static void createArchive(ClassLoader classLoader, File outputFile) throws IOException {
        Set<String> addedEntries = new HashSet<>();
        Set<File> classpathFiles = collectClasspathFiles(classLoader);

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream jos = new JarOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputFile), BUFFER_SIZE), manifest)) {
            // Disable compression for speed (STORED instead of DEFLATED)
            jos.setLevel(ZipOutputStream.STORED);
            for (File file : classpathFiles) {
                addFileToArchive(file, jos, addedEntries);
            }
        }
    }

    /**
     * Writes a single file entry into the given {@link JarOutputStream} using
     * uncompressed ({@code STORED}) encoding.
     *
     * <p>The CRC-32 checksum required by the ZIP specification for {@code STORED}
     * entries is computed automatically from {@code data}.</p>
     *
     * @param jos       the target JAR output stream
     * @param entryName the ZIP entry name (i.e. the path inside the archive),
     *                  using forward slashes as separators
     * @param data      the raw bytes to store as the entry content
     * @throws IOException if an I/O error occurs while writing to {@code jos}
     */
    private static void addEntryToJar(JarOutputStream jos, String entryName, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(data.length);
        entry.setCompressedSize(data.length);
        CRC32 crc = new CRC32();
        crc.update(data);
        entry.setCrc(crc.getValue());
        jos.putNextEntry(entry);
        jos.write(data);
        jos.closeEntry();
    }

    /**
     * Writes an empty directory entry into the given {@link JarOutputStream}.
     *
     * <p>Directory entries are required by certain tools to correctly reconstruct
     * a directory hierarchy from a JAR. They are written as zero-byte
     * {@code STORED} entries whose names end with {@code /}.</p>
     *
     * @param jos       the target JAR output stream
     * @param entryName the directory entry name; must end with {@code /} and use
     *                  forward slashes as separators
     * @throws IOException if an I/O error occurs while writing to {@code jos}
     */
    private static void addDirectoryEntryToJar(JarOutputStream jos, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(0);
        entry.setCompressedSize(0);
        entry.setCrc(0);
        jos.putNextEntry(entry);
        jos.closeEntry();
    }

    /**
     * Collects all classpath roots (directories and JAR files) that are visible
     * from the given classloader and its entire parent chain.
     *
     * <p>Each classloader in the hierarchy is inspected: if it is an instance of
     * {@link URLClassLoader} its URLs are converted to {@link File} objects and
     * added to the result set. Non-existent paths are silently ignored.</p>
     *
     * <p>If the collected set contains no directory that looks like a compiled
     * output directory (i.e. whose path contains {@code classes} or
     * {@code resources}), the method falls back to parsing the
     * {@code java.class.path} system property so that applications launched
     * without a {@link URLClassLoader} (e.g. with the built-in application
     * classloader on Java 9+) are still handled correctly.</p>
     *
     * @param classLoader the starting classloader; may be {@code null}, in which
     *                    case the fallback to {@code java.class.path} applies
     * @return an ordered set of existing {@link File} objects representing
     *         classpath roots, in delegation order (child before parent)
     */
    private static Set<File> collectClasspathFiles(ClassLoader classLoader) {
        Set<File> files = new LinkedHashSet<>();

        ClassLoader current = classLoader;
        while (current != null) {
            if (current instanceof URLClassLoader) {
                // Classpath URLs are expected to be local files in this context (JUnit/Surefire/Gradle),
                // but we guard against edge cases such as: non-existent speculative entries added by IDEs
                // or build tools before compilation, remote URLs (http://) used by custom classloaders,
                // or nested JAR URLs (jar:file:/.../outer.jar!/inner) that cannot be mapped to a File.
                for (java.net.URL url : ((URLClassLoader) current).getURLs()) {
                    try {
                        File f = new File(url.toURI());
                        if (f.exists()) files.add(f);
                    } catch (java.net.URISyntaxException e) {
                        // Fallback to getPath() for malformed URLs that aren't valid URIs
                        File f = new File(url.getPath());
                        if (f.exists()) files.add(f);
                    }
                }
            }
            current = current.getParent();
        }

        if (files.isEmpty() || !containsClassesDirectory(files)) {
            String classpath = System.getProperty("java.class.path", "");
            for (String entry : classpath.split(File.pathSeparator)) {
                if (!entry.isBlank()) {
                    File f = new File(entry);
                    if (f.exists()) files.add(f);
                }
            }
        }

        return files;
    }

    /**
     * Returns {@code true} if {@code files} contains at least one directory
     * whose path includes the token {@code classes} or {@code resources}.
     *
     * <p>This heuristic is used to decide whether the classloader hierarchy
     * already exposes compiled output directories, or whether the fallback
     * to the {@code java.class.path} system property is needed.</p>
     *
     * @param files the set of classpath roots to inspect
     * @return {@code true} if a classes/resources directory is present,
     *         {@code false} otherwise
     */
    private static boolean containsClassesDirectory(Set<File> files) {
        return files.stream().anyMatch(f -> f.isDirectory() &&
                (f.getPath().contains("classes") || f.getPath().contains("resources")));
    }

    /**
     * Adds the contents of a single classpath root to the archive.
     *
     * <p>The behaviour depends on the type of {@code file}:</p>
     * <ul>
     *   <li>If it is a <em>directory</em>, its entire subtree is merged via
     *       {@link #addDirectoryToArchive}.</li>
     *   <li>If it is a <em>JAR file</em> (name ends with {@code .jar}), its
     *       entries are flattened into the output via {@link #addJarToArchive}.</li>
     *   <li>Any other file type is silently ignored.</li>
     * </ul>
     *
     * @param file         the classpath root to add
     * @param jos          the target JAR output stream
     * @param addedEntries a mutable set of entry names already written to
     *                     {@code jos}; used to prevent duplicates
     * @throws IOException if an I/O error occurs while reading {@code file}
     *                     or writing to {@code jos}
     */
    private static void addFileToArchive(File file, JarOutputStream jos, Set<String> addedEntries) throws IOException {
        if (file.isDirectory()) {
            addDirectoryToArchive(file.toPath(), file.toPath(), jos, addedEntries);
        } else if (file.getName().endsWith(".jar")) {
            addJarToArchive(file, jos, addedEntries);
        }
    }

    /**
     * Recursively walks a directory tree rooted at {@code current} and adds every
     * file and sub-directory to the archive, using paths relative to {@code root}.
     *
     * <p>Directory entries are written before their children. Back-slashes in
     * Windows paths are normalised to forward slashes so that the archive is
     * portable across operating systems. Entries whose relative name is already
     * present in {@code addedEntries} are skipped.</p>
     *
     * @param root         the classpath root directory; used to compute relative
     *                     entry names
     * @param current      the directory to walk (initially equal to {@code root})
     * @param jos          the target JAR output stream
     * @param addedEntries a mutable set of entry names already written to
     *                     {@code jos}; updated in place as new entries are added
     * @throws IOException if an I/O error occurs while traversing the directory
     *                     or writing to {@code jos}
     */
    private static void addDirectoryToArchive(Path root, Path current, JarOutputStream jos, Set<String> addedEntries) throws IOException {
        Files.walkFileTree(current, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(root)) {
                    String entryName = root.relativize(dir).toString().replace("\\", "/") + "/";
                    if (addedEntries.add(entryName)) {
                        addDirectoryEntryToJar(jos, entryName);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String entryName = root.relativize(file).toString().replace("\\", "/");
                if (addedEntries.add(entryName)) {
                    addEntryToJar(jos, entryName, Files.readAllBytes(file));
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Unpacks the entries of an existing JAR file and merges them into the output
     * archive, skipping {@code META-INF/MANIFEST.MF} and any duplicate entries.
     *
     * <p>The manifest is excluded because the output archive has its own minimal
     * manifest; including original manifests from dependency JARs could create
     * conflicting or misleading metadata. All other entries — including other
     * files under {@code META-INF/} — are copied verbatim.</p>
     *
     * @param jarFile      the source JAR file to unpack
     * @param jos          the target JAR output stream
     * @param addedEntries a mutable set of entry names already written to
     *                     {@code jos}; updated in place as new entries are added
     * @throws IOException if an I/O error occurs while reading {@code jarFile}
     *                     or writing to {@code jos}
     */
    private static void addJarToArchive(File jarFile, JarOutputStream jos, Set<String> addedEntries) throws IOException {
        try (JarInputStream jis = new JarInputStream(
                new BufferedInputStream(new FileInputStream(jarFile), BUFFER_SIZE))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                String entryName = entry.getName();
                if (entryName.equalsIgnoreCase("META-INF/MANIFEST.MF")) {
                    continue;
                }
                if (addedEntries.add(entryName)) {
                    addEntryToJar(jos, entryName, jis.readAllBytes());
                }
                jis.closeEntry();
            }
        }
    }
}