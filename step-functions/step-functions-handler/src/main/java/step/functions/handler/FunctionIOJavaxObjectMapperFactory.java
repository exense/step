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
package step.functions.handler;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import step.functions.io.Input;
import step.functions.io.Output;

/**
 * Factory used to create the {@link ObjectMapper} used to serialize/deserialize
 * {@link Input} and {@link Output} instances (legacy, javax.json, version)
 *
 */
public class FunctionIOJavaxObjectMapperFactory {

    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // We need to use reflection as these artifacts are (knowingly) declared as a runtime dependency, to discourage use of legacy javax.json packages.
            SimpleModule jsr353Module = (SimpleModule) Class.forName("com.fasterxml.jackson.datatype.jsr353.JSR353Module").getDeclaredConstructor().newInstance();
            mapper.registerModule(jsr353Module);
        } catch (Exception e) {
            // Unexpected, this class should always be present as it's declared as a runtime dependency.
            throw new RuntimeException(e);
        }
        return mapper;
    }
}
