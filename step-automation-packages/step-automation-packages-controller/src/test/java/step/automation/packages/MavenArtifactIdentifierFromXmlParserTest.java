package step.automation.packages;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.Test;
import step.core.maven.MavenArtifactIdentifier;
import step.core.maven.MavenArtifactIdentifierFromXmlParser;

public class MavenArtifactIdentifierFromXmlParserTest {

    @Test
    public void parse() throws JsonProcessingException {
        MavenArtifactIdentifierFromXmlParser parser = new MavenArtifactIdentifierFromXmlParser();
        String xml = "<dependency>\n" +
                "<groupId>org.junit.jupiter</groupId>\n" +
                "<artifactId>junit-jupiter-api</artifactId>\n" +
                "<version>5.12.1</version>\n" +
                "<scope>test</scope>\n" +
                "<classifier>tests</classifier>\n" +
                "</dependency>";
        MavenArtifactIdentifier result = parser.parse(xml);

        Assert.assertEquals("org.junit.jupiter", result.getGroupId());
        Assert.assertEquals("junit-jupiter-api", result.getArtifactId());
        Assert.assertEquals("5.12.1", result.getVersion());
        Assert.assertEquals("tests", result.getClassifier());
    }
}