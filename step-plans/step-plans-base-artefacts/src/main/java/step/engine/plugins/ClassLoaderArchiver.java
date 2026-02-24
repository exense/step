package step.engine.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

/**
 * Utility class for creating a flat JAR archive that bundles all classpath entries
 * visible to the current JVM process.
 *
 * <p>The archive is produced by parsing the {@code java.class.path} system property
 * and merging every discovered classpath entry — either a directory tree or an existing
 * JAR file — into a single output JAR. This covers all standard Java 11+ execution
 * contexts (IDE, Maven, Gradle). Entries are stored uncompressed ({@link ZipEntry#STORED})
 * for maximum throughput. Duplicate entries (same relative path) are silently skipped
 * so that the first occurrence wins, which mirrors the standard classloader delegation
 * model.</p>
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
     * <p>The method collects every classpath root (directories and JAR files) from the
     * {@code java.class.path} system property and merges them into one flat JAR.
     * This covers all standard Java 11+ JUnit execution contexts (IDE, Maven, Gradle).
     * The archive uses uncompressed ({@code STORED}) entries to minimise CPU overhead
     * during creation.</p>
     *
     * @param outputFile  the file to write the archive to; it is created or
     *                    overwritten if it already exists
     * @param resourcesOnly whether the built JAR should only include resources files (i.e. skip classes and META-INF files
     * @throws IOException if an I/O error occurs while reading classpath entries
     *                     or writing the output file
     */
    public static void createArchive(File outputFile, boolean resourcesOnly) throws IOException {
        Set<String> addedEntries = new HashSet<>();
        Set<File> classpathFiles = collectClasspathFiles();

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream jos = new JarOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputFile), BUFFER_SIZE), manifest)) {
            // Disable compression for speed (STORED instead of DEFLATED)
            jos.setLevel(ZipOutputStream.STORED);
            for (File file : classpathFiles) {
                addFileToArchive(file, jos, addedEntries, resourcesOnly);
            }
        }
    }

    /**
     * Streams a single file entry into the given {@link JarOutputStream} using
     * uncompressed ({@code STORED}) encoding.
     *
     * <p>The ZIP specification requires size and CRC-32 to appear in the entry
     * header, before the data. Both values must therefore be known before this
     * method is called; callers are responsible for supplying them (e.g. from
     * filesystem metadata or the source JAR's central directory).
     * Content is copied from {@code source} in chunks of {@value #BUFFER_SIZE}
     * bytes so that no large intermediate buffer is allocated.</p>
     *
     * @param jos       the target JAR output stream
     * @param entryName the ZIP entry name (i.e. the path inside the archive),
     *                  using forward slashes as separators
     * @param source    stream supplying the raw (uncompressed) entry content;
     *                  the caller is responsible for closing it
     * @param size      uncompressed size in bytes
     * @param crc       CRC-32 of the uncompressed content
     * @throws IOException if an I/O error occurs while writing to {@code jos}
     */
    private static void addEntryToJar(JarOutputStream jos, String entryName, InputStream source, long size, long crc) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(size);
        entry.setCompressedSize(size);
        entry.setCrc(crc);
        jos.putNextEntry(entry);
        byte[] buf = new byte[BUFFER_SIZE];
        int read;
        while ((read = source.read(buf)) != -1) {
            jos.write(buf, 0, read);
        }
        jos.closeEntry();
    }

    /**
     * Computes the CRC-32 of a file by streaming its content, using a
     * {@value #BUFFER_SIZE}-byte read buffer.
     *
     * @param file the file to checksum
     * @return the CRC-32 value of the file's content
     * @throws IOException if an I/O error occurs while reading the file
     */
    private static long computeCrc(Path file) throws IOException {
        CRC32 crc = new CRC32();
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buf = new byte[BUFFER_SIZE];
            int read;
            while ((read = is.read(buf)) != -1) {
                crc.update(buf, 0, read);
            }
        }
        return crc.getValue();
    }

    /**
     * Collects all classpath roots (directories and JAR files) by parsing the
     * {@code java.class.path} system property.
     *
     * <p>Non-existent paths are silently ignored.</p>
     *
     * @return an ordered set of existing {@link File} objects representing
     *         classpath roots, in the order they appear on the classpath
     */
    private static Set<File> collectClasspathFiles() {
        Set<File> files = new LinkedHashSet<>();

        String classpath = System.getProperty("java.class.path", "");
        for (String entry : classpath.split(File.pathSeparator)) {
            if (!entry.isBlank()) {
                File f = new File(entry);
                if (f.exists()) files.add(f);
            }
        }

        return files;
    }

    /**
     * Returns {@code true} if {@code files} contains at least one directory
     * whose path includes the token {@code classes} or {@code resources}.
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
     * @param resourcesOnly whether the built JAR should only include resources files (i.e. skip classes and META-INF files
     * @throws IOException if an I/O error occurs while reading {@code file}
     *                     or writing to {@code jos}
     */
    private static void addFileToArchive(File file, JarOutputStream jos, Set<String> addedEntries, boolean resourcesOnly) throws IOException {
        if (file.isDirectory()) {
            addDirectoryToArchive(file.toPath().toAbsolutePath().normalize(), jos, addedEntries, resourcesOnly);
        } else if (file.getName().endsWith(".jar")) {
            addJarToArchive(file, jos, addedEntries, resourcesOnly);
        }
    }

    /**
     * Recursively walks the directory tree rooted at {@code root} and adds every
     * file to the archive using paths relative to {@code root}.
     *
     * <p>{@code root} must be an absolute, normalised path so that
     * {@link Path#relativize} always produces the correct sub-path regardless of
     * how the original classpath entry was expressed (relative, with {@code .} or
     * {@code ..} components, etc.). Back-slashes in Windows paths are normalised
     * to forward slashes so that the archive is portable across operating systems.
     * Directory entries are intentionally omitted; Java's {@link ClassLoader} does
     * not require them to locate resources inside a JAR. Entries whose relative
     * name is already present in {@code addedEntries} are skipped.</p>
     *
     * <p>Each file is read twice: once to compute its CRC-32 (required by the ZIP
     * specification in the STORED entry header before the data), and once to stream
     * its content into the archive. Only a {@value #BUFFER_SIZE}-byte window is
     * held in memory at any time, regardless of file size.</p>
     *
     * @param root          the absolute, normalised classpath root directory; used
     *                      both as the walk starting point and to compute relative
     *                      entry names
     * @param jos           the target JAR output stream
     * @param addedEntries  a mutable set of entry names already written to
     *                      {@code jos}; updated in place as new entries are added
     * @param resourcesOnly whether to apply the same exclusion rules as
     *                      {@link #shouldExcludeFromJar} (skip {@code .class} files
     *                      and non-services {@code META-INF} content)
     * @throws IOException if an I/O error occurs while traversing the directory
     *                     or writing to {@code jos}
     */
    private static void addDirectoryToArchive(Path root, JarOutputStream jos, Set<String> addedEntries, boolean resourcesOnly) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String entryName = root.relativize(file).toString().replace("\\", "/");
                if (!shouldExcludeFromJar(entryName, resourcesOnly) && addedEntries.add(entryName)) {
                    long crc = computeCrc(file);
                    try (InputStream is = Files.newInputStream(file)) {
                        addEntryToJar(jos, entryName, is, attrs.size(), crc);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Unpacks the entries of an existing JAR file and merges them into the output
     * archive, skipping directory entries, {@code META-INF/MANIFEST.MF}, and any
     * duplicate entries.
     *
     * <p>This method uses {@link JarFile} (random-access) rather than
     * {@link java.util.jar.JarInputStream} (sequential) so that the uncompressed
     * size and CRC-32 of every entry can be read directly from the ZIP central
     * directory. This means entry content is streamed in fixed-size chunks
     * ({@value #BUFFER_SIZE} bytes) without loading any entry fully into
     * memory, regardless of compression method or entry size.</p>
     *
     * <p>Directory entries (names ending with {@code /}) are omitted because
     * Java's {@link ClassLoader} does not need them to locate resources, and
     * keeping them would produce spurious empty entries in the output archive.
     * The manifest is excluded because the output archive has its own minimal
     * manifest; including original manifests from dependency JARs could create
     * conflicting or misleading metadata. All other entries — including other
     * files under {@code META-INF/} — are copied verbatim.</p>
     *
     * @param jarFile       the source JAR file to unpack
     * @param jos           the target JAR output stream
     * @param addedEntries  a mutable set of entry names already written to
     *                      {@code jos}; updated in place as new entries are added
     * @param resourcesOnly whether the built JAR should only include resources files (i.e. skip classes and META-INF files
     * @throws IOException if an I/O error occurs while reading {@code jarFile}
     *                     or writing to {@code jos}
     */
    private static void addJarToArchive(File jarFile, JarOutputStream jos, Set<String> addedEntries, boolean resourcesOnly) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (!entry.isDirectory() && !shouldExcludeFromJar(entryName, resourcesOnly) && addedEntries.add(entryName)) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        addEntryToJar(jos, entryName, is, entry.getSize(), entry.getCrc());
                    }
                }
            }
        }
    }

    /**
     * Filter out META-INF files of existing JAR when unpacking and building a single fatJAR
     * <ul>
     *     <li>MANIFEST.MF: manifest files of existing JARs, we create our own</li>
     *     <li>.SF, .DSA, .RSA, .EC: signature files that would not be valid for the fatJAR</li>
     * </ul>
     * If resourcesOnly is true, the all classes and other unrequired NETA-INF content are excluded too
     * @param entryName the entry to be checked
     * @param resourcesOnly whether the built JAR should only include resources files (i.e. skip classes and META-INF files
     * @return whether the entry should be excluded
     */
    private static boolean shouldExcludeFromJar(String entryName, boolean resourcesOnly) {
        if (resourcesOnly) {
            // Skip .class files
            if (entryName.endsWith(".class")) {
                return true;
            }
            // Skip META-INF except potentially useful runtime files like services or manifests
            if (entryName.startsWith("META-INF/")
                    && !entryName.startsWith("META-INF/services/")) {
                return true;
            }
            return false;
        } else {
            //Even for the non resourcesOnly mode, we need to exclude some of the manifest files of individual JARs
            // Skip manifest — we generate our own
            if (entryName.equalsIgnoreCase("META-INF/MANIFEST.MF")) {
                return true;
            }
            // Skip JAR signing files — merging signed JARs into a fat JAR invalidates
            // the signatures and causes SecurityException at runtime when the JVM
            // tries to verify them against the new merged manifest
            if (entryName.startsWith("META-INF/")) {
                String upperName = entryName.toUpperCase();
                if (upperName.endsWith(".SF")   // signature file
                        || upperName.endsWith(".DSA")  // DSA signature block
                        || upperName.endsWith(".RSA")  // RSA signature block
                        || upperName.endsWith(".EC")) {// EC signature block
                    return true;
                }
            }
            return false;
        }
    }
}