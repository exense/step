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
                "</settings>", tempDirectory.toFile());
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
                "</settings>", tempDirectory.toFile());
        ResolvedMavenArtifact resolvedMavenArtifact = mavenArtifactClient.getArtifact(new DefaultArtifact("org.apache.maven", "maven-artifact", "jar", "4.1.0-SNAPSHOT"));
        Assert.assertNotNull(resolvedMavenArtifact.artifactFile);
        Assert.assertNotNull(resolvedMavenArtifact.snapshotMetadata);
        Assert.assertTrue(resolvedMavenArtifact.snapshotMetadata.timestamp >= 1758190231000L); //last Updated is not always accurate that would be far in the future
    }

}