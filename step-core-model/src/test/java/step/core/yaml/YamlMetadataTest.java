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
package step.core.yaml;

import org.junit.Test;
import step.core.accessors.AbstractOrganizableObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class YamlMetadataTest {

    @Test
    public void applyAndExtract() {
        AbstractOrganizableObject object = new AbstractOrganizableObject();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("owner", "team-a");
        metadata.put("nested", Map.of("list", List.of(Map.of("key", 1), "value"), "not$leading", true));
        metadata.put("empty", null);

        YamlMetadata.applyTo(object, metadata);

        assertSame(metadata, object.getCustomField(YamlMetadata.METADATA_FIELD));
        assertSame(metadata, YamlMetadata.extractFrom(object));
    }

    @Test
    public void nullOrEmptyMetadataIsNotApplied() {
        AbstractOrganizableObject object = new AbstractOrganizableObject();
        YamlMetadata.applyTo(object, null);
        YamlMetadata.applyTo(object, Map.of());
        assertNull(object.getCustomFields());
        assertNull(YamlMetadata.extractFrom(object));
    }

    @Test
    public void invalidKeys() {
        assertInvalid(Map.of("a.b", "value"), "Invalid key 'a.b' in metadata");
        assertInvalid(Map.of("$a", "value"), "Invalid key '$a' in metadata");
        assertInvalid(Map.of("", "value"), "Invalid key '' in metadata");
        assertInvalid(Map.of("a", Map.of("b.c", "value")), "Invalid key 'b.c' in metadata.a");
        assertInvalid(Map.of("a", List.of("value", Map.of("$b", "value"))), "Invalid key '$b' in metadata.a[1]");
    }

    private static void assertInvalid(Map<String, Object> metadata, String expectedMessagePrefix) {
        AbstractOrganizableObject object = new AbstractOrganizableObject();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> YamlMetadata.applyTo(object, metadata));
        assertTrue(e.getMessage(), e.getMessage().startsWith(expectedMessagePrefix));
        assertNull(object.getCustomFields());
    }
}
