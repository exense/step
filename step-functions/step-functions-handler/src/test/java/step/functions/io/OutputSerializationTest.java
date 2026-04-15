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
package step.functions.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.junit.Test;
import step.functions.handler.FunctionIOJakartaObjectMapperFactory;
import step.functions.handler.FunctionIOJavaxObjectMapperFactory;

import java.io.IOException;

public class OutputSerializationTest {

    @Test
    public void test() throws IOException {
        OutputBuilder builder = new OutputBuilder();
        builder.add("test", "test");
        Output<JsonObject> output = builder.build();
        ObjectMapper javaxMapper = FunctionIOJavaxObjectMapperFactory.createObjectMapper();
        String value = javaxMapper.writeValueAsString(output);
        javaxMapper.readValue(value, new TypeReference<Output<javax.json.JsonObject>>() {
        });

        ObjectMapper jakartaMapper = FunctionIOJakartaObjectMapperFactory.createObjectMapper();
        jakartaMapper.readValue(value, new TypeReference<Output<jakarta.json.JsonObject>>() {
        });
    }
}
