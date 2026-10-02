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
package step.core.filebrowser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import step.core.accessors.DefaultJacksonMapperProvider;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Pins the JSON the browse services emit. Records are serialised under their <i>component</i> names,
 * which is easy to get wrong when reading the java side only: {@code boolean directory()} is
 * {@code "directory"}, not {@code "isDirectory"}. The frontend binds to these keys.
 */
public class FileDescriptorTest {

    private final ObjectMapper mapper = DefaultJacksonMapperProvider.getObjectMapper();

    @Test
    public void serialisesTheEntryUnderItsComponentNames() throws Exception {
        FileDescriptor entry = new FileDescriptor("pool.csv", "data/pool.csv", false, true, false, false,
            42L, "apResource:apA:data/pool.csv");

        Map<String, Object> json = mapper.readValue(mapper.writeValueAsString(entry), new TypeReference<>() {
        });

        assertEquals(Map.of(
            "name", "pool.csv",
            "path", "data/pool.csv",
            "directory", false,
            "regularFile", true,
            "hidden", false,
            "symlink", false,
            "size", 42,
            "resourceReference", "apResource:apA:data/pool.csv"
        ), json);
    }

    @Test
    public void serialisesTheDirectoryListingUnderItsComponentNames() throws Exception {
        DirectoryListing content = new DirectoryListing("data", "", "apResource:apA:data", List.of());

        Map<String, Object> json = mapper.readValue(mapper.writeValueAsString(content), new TypeReference<>() {
        });

        assertEquals(Map.of(
            "path", "data",
            "parentPath", "",
            "resourceReference", "apResource:apA:data",
            "entries", List.of()
        ), json);
    }
}
