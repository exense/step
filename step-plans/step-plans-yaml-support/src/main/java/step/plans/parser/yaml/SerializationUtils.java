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
package step.plans.parser.yaml;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class SerializationUtils {

    public static List<String> getJsonFieldNames(ObjectMapper objectMapper, Class<?> clazz) {
        try {
            JsonSerializer<Object> serializer = objectMapper.getSerializerProviderInstance().findValueSerializer(clazz);
            List<String> res = new ArrayList<>();
            serializer.properties().forEachRemaining(p -> res.add(p.getName()));
            return res;
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        }
    }
}
