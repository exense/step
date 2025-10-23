package step.repositories.artifact;

import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

public class MavenArtifactClientTest {

    @Test
    public void test() throws SettingsBuildingException, ArtifactResolutionException, IOException {
        Path tempDirectory = Files.createTempDirectory("maven-repo-test");
        MavenArtifactClient mavenArtifactClient = new MavenArtifactClient("<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "  xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd\">\n" +
                "    <profiles>\n" +
                "        <profile>\n" +
                "            <id>default</id>\n" +
                "            <repositories>\n" +
                "\t        <repository>\n" +
                "\t            <id>central</id>\n" +
                "\t            <name>Central</name>\n" +
                "\t            <url>https://repo1.maven.org/maven2/</url>\n" +
                "\t            <releases>\n" +
                "\t                <enabled>true</enabled>\n" +
                "\t                <updatePolicy>never</updatePolicy>\n" +
                "\t            </releases>\n" +
                "\t            <snapshots>\n" +
                "\t                <enabled>false</enabled>\n" +
                "\t            </snapshots>\n" +
                "\t        </repository>\n" +
                "\t    </repositories>\n" +
                "        </profile>\n" +
                "    </profiles>\n" +
                "    <activeProfiles>\n" +
                "        <activeProfile>default</activeProfile>\n" +
                "    </activeProfiles>\n" +
                "</settings>", tempDirectory.toFile(), null, null);
        ResolvedMavenArtifact resolvedMavenArtifact = mavenArtifactClient.getArtifact(new DefaultArtifact("ch.exense.step", "step-api", "pom", "1.1.9"));
        Assert.assertNotNull(resolvedMavenArtifact.artifactFile);
        Assert.assertNull(resolvedMavenArtifact.snapshotMetadata);
    }

    @Test
    public void testSnapshot() throws SettingsBuildingException, ArtifactResolutionException, IOException {
        Path tempDirectory = Files.createTempDirectory("maven-repo-test");
        MavenArtifactClient mavenArtifactClient = new MavenArtifactClient("<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 " +
                "https://maven.apache.org/xsd/settings-1.0.0.xsd\">\n" +
                "    <profiles>\n" +
                "        <profile>\n" +
                "            <id>default</id>\n" +
                "            <repositories>\n" +
                "                <repository>\n" +
                "                    <id>apache-snapshots</id>\n" +
                "                    <name>Apache Snapshots</name>\n" +
                "                    <url>https://repository.apache.org/content/repositories/snapshots/</url>\n" +
                "                    <snapshots>\n" +
                "                        <enabled>true</enabled>\n" +
                "                        <updatePolicy>always</updatePolicy>\n" +
                "                    </snapshots>\n" +
                "                    <releases>\n" +
                "                        <enabled>false</enabled>\n" +
                "                    </releases>\n" +
                "                </repository>\n" +
                "            </repositories>\n" +
                "        </profile>\n" +
                "    </profiles>\n" +
                "    <activeProfiles>\n" +
                "        <activeProfile>default</activeProfile>\n" +
                "    </activeProfiles>\n" +
                "</settings>", tempDirectory.toFile(), null, null);
        ResolvedMavenArtifact resolvedMavenArtifact = mavenArtifactClient.getArtifact(new DefaultArtifact("org.apache.maven", "maven-artifact", "jar", "4.1.0-SNAPSHOT"));
        Assert.assertNotNull(resolvedMavenArtifact.artifactFile);
        Assert.assertNotNull(resolvedMavenArtifact.snapshotMetadata);
        Assert.assertTrue(resolvedMavenArtifact.snapshotMetadata.timestamp >= 1758190231000L); //last Updated is not always accurate that would be far in the future
    }

    @Test
    public void testEnableDisableCleanup() throws SettingsBuildingException, IOException {
        Path tempDirectory = Files.createTempDirectory("maven-repo-cleanup-test");
        MavenArtifactClient client = createTestClient(tempDirectory, Duration.ofDays(30), Duration.ofHours(12));

        Assert.assertTrue("Cleanup should be enabled by default", client.isCleanupEnabled());
        client.disableAutomaticCleanup();
        Assert.assertFalse("Cleanup should be disabled after calling enableAutomaticCleanup", client.isCleanupEnabled());

        client = createTestClient(tempDirectory, null, null);
        Assert.assertFalse("Cleanup should be disabled after calling disableAutomaticCleanup", client.isCleanupEnabled());

        client = createTestClient(tempDirectory, Duration.ofHours(0), null);
        Assert.assertFalse("Cleanup should be disabled after calling disableAutomaticCleanup", client.isCleanupEnabled());
    }

    @Test
    public void testCleanupMultipleInstances() throws SettingsBuildingException, IOException {
        Path tempDirectory = Files.createTempDirectory("maven-repo-shared-test");

        MavenArtifactClient client1 = createTestClient(tempDirectory, Duration.ofDays(30), Duration.ofHours(12));
        MavenArtifactClient client2 = createTestClient(tempDirectory, Duration.ofDays(15), Duration.ofHours(6));

        Assert.assertTrue("Client1 cleanup should be enabled", client1.isCleanupEnabled());
        Assert.assertTrue("Client2 cleanup should be enabled", client2.isCleanupEnabled());

        MavenCacheCleanupScheduler scheduler = MavenCacheCleanupScheduler.getInstance();
        Assert.assertEquals("Should have 1 registered repository (shared)", 1, scheduler.getRegisteredRepositoriesCount());

        client1.disableAutomaticCleanup();
        Assert.assertFalse("Client1 cleanup should be disabled", client1.isCleanupEnabled());
        Assert.assertTrue("Client2 cleanup should still be enabled", client2.isCleanupEnabled());

        client2.disableAutomaticCleanup();
        Assert.assertFalse("Client2 cleanup should be disabled", client2.isCleanupEnabled());
    }

    @Test
    public void testCleanupWithOldFiles() throws SettingsBuildingException, IOException, InterruptedException {
        Path tempDirectory = Files.createTempDirectory("maven-repo-oldfiles-test");

        // Create Maven repository structure: groupId/artifactId/version/
        Path oldVersionDir = tempDirectory.resolve("com").resolve("example").resolve("artifact").resolve("1.0.0");
        Path newVersionDir = tempDirectory.resolve("com").resolve("example").resolve("artifact").resolve("2.0.0");
        Path mixedVersionDir = tempDirectory.resolve("com").resolve("example").resolve("artifact").resolve("1.5.0");

        Files.createDirectories(oldVersionDir);
        Files.createDirectories(newVersionDir);
        Files.createDirectories(mixedVersionDir);

        Instant oldTime = Instant.now().minus(Duration.ofDays(10));
        FileTime oldFileTime = FileTime.from(oldTime);

        // Old version directory - ALL files are old
        Path oldJar = oldVersionDir.resolve("artifact-1.0.0.jar");
        Path oldPom = oldVersionDir.resolve("artifact-1.0.0.pom");
        Files.write(oldJar, "old jar content".getBytes());
        Files.write(oldPom, "old pom content".getBytes());

        // Set both last modified and last access time for old files
        Files.setLastModifiedTime(oldJar, oldFileTime);
        Files.setLastModifiedTime(oldPom, oldFileTime);
        try {
            Files.setAttribute(oldJar, "lastAccessTime", oldFileTime);
            Files.setAttribute(oldPom, "lastAccessTime", oldFileTime);
        } catch (Exception e) {
            // Ignore if not supported on this file system
        }

        // New version directory - ALL files are new
        Path newJar = newVersionDir.resolve("artifact-2.0.0.jar");
        Path newPom = newVersionDir.resolve("artifact-2.0.0.pom");
        Files.write(newJar, "new jar content".getBytes());
        Files.write(newPom, "new pom content".getBytes());

        // Mixed version directory - SOME files old, SOME files new
        Path mixedOldJar = mixedVersionDir.resolve("artifact-1.5.0.jar");
        Path mixedNewPom = mixedVersionDir.resolve("artifact-1.5.0.pom");
        Files.write(mixedOldJar, "mixed old jar".getBytes());
        Files.write(mixedNewPom, "mixed new pom".getBytes());

        // Set old file timestamp
        Files.setLastModifiedTime(mixedOldJar, oldFileTime);
        try {
            Files.setAttribute(mixedOldJar, "lastAccessTime", oldFileTime);
        } catch (Exception e) {
            // Ignore if not supported on this file system
        }
        // mixedNewPom keeps current timestamp (new)

        // Enable cleanup: delete files older than 5 days, check every 50ms
        MavenArtifactClient client = createTestClient(tempDirectory, Duration.ofDays(5), Duration.ofMillis(50));

        // Wait up to 2 seconds for cleanup to happen
        boolean cleanupCompleted = false;
        for (int i = 0; i < 40; i++) {
            Thread.sleep(50);

            // Check if cleanup has completed correctly
            boolean oldVersionDeleted = !Files.exists(oldJar) && !Files.exists(oldPom);
            boolean newVersionExists = Files.exists(newJar) && Files.exists(newPom);
            boolean mixedVersionUntouched = Files.exists(mixedOldJar) && Files.exists(mixedNewPom);

            if (oldVersionDeleted && newVersionExists && mixedVersionUntouched) {
                cleanupCompleted = true;
                break;
            }
        }

        Assert.assertTrue("Cleanup should complete within 2 seconds", cleanupCompleted);

        // Verify correct behavior:
        // 1. Old version directory: ALL files deleted (because all were old)
        Assert.assertFalse("Old version JAR should be deleted", Files.exists(oldJar));
        Assert.assertFalse("Old version POM should be deleted", Files.exists(oldPom));

        // 2. New version directory: ALL files preserved (because all were new)
        Assert.assertTrue("New version JAR should be preserved", Files.exists(newJar));
        Assert.assertTrue("New version POM should be preserved", Files.exists(newPom));

        // 3. Mixed version directory: NO files deleted (because not all were old)
        Assert.assertTrue("Mixed version old JAR should NOT be deleted (directory has new files)", Files.exists(mixedOldJar));
        Assert.assertTrue("Mixed version new POM should be preserved", Files.exists(mixedNewPom));

        client.disableAutomaticCleanup();
    }

    @Test
    public void testCleanupInvalidParameters() throws SettingsBuildingException, IOException {
        Path tempDirectory = Files.createTempDirectory("maven-repo-invalid-test");
        MavenArtifactClient client = createTestClient(tempDirectory,null ,null);

        try {
            client.enableAutomaticCleanup(null, Duration.ofDays(1));
            Assert.fail("Should throw IllegalArgumentException for null maxAge");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Should mention null parameter", e.getMessage().contains("cannot be null"));
        }

        try {
            client.enableAutomaticCleanup(Duration.ofDays(1), null);
            Assert.fail("Should throw IllegalArgumentException for null frequency");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Should mention null parameter", e.getMessage().contains("cannot be null"));
        }

        try {
            client.enableAutomaticCleanup(Duration.ofDays(-1), Duration.ofDays(1));
            Assert.fail("Should throw IllegalArgumentException for negative maxAge");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Should mention positive requirement", e.getMessage().contains("must be positive"));
        }

        try {
            client.enableAutomaticCleanup(Duration.ZERO, Duration.ofDays(1));
            Assert.fail("Should throw IllegalArgumentException for zero maxAge");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Should mention positive requirement", e.getMessage().contains("must be positive"));
        }
    }

    @Test
    public void testGetLocalRepository() throws SettingsBuildingException, IOException {
        Path tempDirectory = Files.createTempDirectory("maven-repo-getter-test");
        MavenArtifactClient client = createTestClient(tempDirectory, Duration.ofDays(30), Duration.ofHours(12));

        File localRepo = client.getLocalRepository();
        Assert.assertEquals("Should return the correct local repository", tempDirectory.toFile(), localRepo);
    }

    private MavenArtifactClient createTestClient(Path tempDirectory, Duration maxAge, Duration frequency) throws SettingsBuildingException {
        return new MavenArtifactClient(
            "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 " +
            "https://maven.apache.org/xsd/settings-1.0.0.xsd\">\n" +
            "    <profiles>\n" +
            "        <profile>\n" +
            "            <id>default</id>\n" +
            "            <repositories>\n" +
            "                <repository>\n" +
            "                    <id>central</id>\n" +
            "                    <name>Central</name>\n" +
            "                    <url>https://repo1.maven.org/maven2/</url>\n" +
            "                    <releases>\n" +
            "                        <enabled>true</enabled>\n" +
            "                        <updatePolicy>never</updatePolicy>\n" +
            "                    </releases>\n" +
            "                    <snapshots>\n" +
            "                        <enabled>false</enabled>\n" +
            "                    </snapshots>\n" +
            "                </repository>\n" +
            "            </repositories>\n" +
            "        </profile>\n" +
            "    </profiles>\n" +
            "    <activeProfiles>\n" +
            "        <activeProfile>default</activeProfile>\n" +
            "    </activeProfiles>\n" +
            "</settings>",
            tempDirectory.toFile(), maxAge, frequency
        );
    }

}