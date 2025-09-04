/*
 * ******************************************************************************
 *  * Copyright (C) 2020, exense GmbH
 *  *
 *  * This file is part of STEP
 *  *
 *  * STEP is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * STEP is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *  *****************************************************************************
 */

package step.core.maven;

import org.junit.Assert;
import org.junit.Test;

public class MavenArtifactIdentifierTest {

    @Test
    public void testFromShortString() {
        // test without classifier and type
        MavenArtifactIdentifier parsed = MavenArtifactIdentifier.fromShortString("mvn:myGroupId:myArtifactId:1.0.0-SNAPSHOT");
        Assert.assertEquals("myGroupId", parsed.getGroupId());
        Assert.assertEquals("myArtifactId", parsed.getArtifactId());
        Assert.assertEquals("1.0.0-SNAPSHOT", parsed.getVersion());
        Assert.assertNull(parsed.getType());
        Assert.assertNull(parsed.getClassifier());

        // test with classifier and type
        parsed = MavenArtifactIdentifier.fromShortString("mvn:myGroupId:myArtifactId:1.0.0-SNAPSHOT:tests:jar");
        Assert.assertEquals("myGroupId", parsed.getGroupId());
        Assert.assertEquals("myArtifactId", parsed.getArtifactId());
        Assert.assertEquals("1.0.0-SNAPSHOT", parsed.getVersion());
        Assert.assertEquals("jar", parsed.getType());
        Assert.assertEquals("tests", parsed.getClassifier());
    }

    @Test
    public void testToStringRepresentation() {
        // test without classifier and type
        MavenArtifactIdentifier mai = new MavenArtifactIdentifier("myGroupId", "myArtifactId", "1.0.0-SNAPSHOT", null, null);
        String stringRepresentation = mai.toStringRepresentation();
        Assert.assertEquals("mvn:myGroupId:myArtifactId:1.0.0-SNAPSHOT", stringRepresentation);

        // test with classifier and type
        mai = new MavenArtifactIdentifier("myGroupId", "myArtifactId", "1.0.0-SNAPSHOT", "tests", "jar");
        stringRepresentation = mai.toStringRepresentation();

        Assert.assertEquals("mvn:myGroupId:myArtifactId:1.0.0-SNAPSHOT:tests:jar", stringRepresentation);
    }
}