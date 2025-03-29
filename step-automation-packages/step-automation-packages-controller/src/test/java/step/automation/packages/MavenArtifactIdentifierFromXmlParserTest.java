package step.automation.packages;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;
import step.core.maven.MavenArtifactIdentifier;

import javax.xml.parsers.ParserConfigurationException;

import static org.junit.Assert.*;

public class MavenArtifactIdentifierFromXmlParserTest {

    @Test
    public void parse() throws ParserConfigurationException, SAXException {
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