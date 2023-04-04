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
        File pom = mavenArtifactClient.getArtifact(new DefaultArtifact("ch.exense.step", "step-api", "pom", "1.1.9"));
        Assert.assertNotNull(pom);
    }

}