/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.automation.packages;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import step.core.maven.MavenArtifactIdentifier;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class MavenArtifactIdentifierFromXmlParser {

    public MavenArtifactIdentifier parse(String xmlSnippet) throws ParserConfigurationException, SAXException {
        MavenArtifactIdentifier mavenArtifactIdentifier;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        try (ByteArrayInputStream bis = new ByteArrayInputStream(xmlSnippet.getBytes())) {
            Document doc = dBuilder.parse(bis);
            NodeList childNodes = doc.getChildNodes();
            Node dependency = childNodes.item(0);
            String groupId = null;
            String artifactId = null;
            String versionId = null;
            String classifier = null;
            NodeList dependencyChildren = dependency.getChildNodes();

            for (int i = 0; i < dependencyChildren.getLength(); i++) {
                Node item = dependencyChildren.item(i);
                switch (item.getNodeName()) {
                    case "groupId":
                        groupId = item.getTextContent() == null ? null : item.getTextContent().trim();
                        break;
                    case "artifactId":
                        artifactId = item.getTextContent() == null ? null : item.getTextContent().trim();
                        break;
                    case "version":
                        versionId = item.getTextContent() == null ? null : item.getTextContent().trim();
                        break;
                    case "classifier":
                        classifier = item.getTextContent() == null ? null : item.getTextContent().trim();
                        break;
                }
            }
            mavenArtifactIdentifier = new MavenArtifactIdentifier(groupId, artifactId, versionId, classifier);
        } catch (IOException e) {
            throw new RuntimeException("IO Exception", e);
        }
        return mavenArtifactIdentifier;
    }
}
