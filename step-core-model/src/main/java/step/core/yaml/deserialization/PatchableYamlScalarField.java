package step.core.yaml.deserialization;
/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.core.yaml.PatchingContext;

import java.io.IOException;

public class PatchableYamlScalarField<T> extends PatchableYamlPrimitive<T> {

    private String fieldName;

    public PatchableYamlScalarField(PatchingContext patchingContext, String fieldName, T value) {
        super(patchingContext, value);
        this.fieldName = fieldName;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public PatchableYamlScalarField(@JacksonInject(useInput = OptBoolean.FALSE) PatchingContext context, T value) {
        this(context, null, value);
    }

    @JsonValue
    @Override
    public JsonNode getJsonValue() {
        ObjectNode node = getPatchingContext().getMapper().createObjectNode();
        if (fieldName != null && value != null) {
            node.putPOJO(fieldName, value);
        }
        return node;
    }

    @Override
    public void onParsed(JsonLocation startLocation, PatchingParserDelegate parserDelegate) {
        try {
            fieldName = parserDelegate.currentName();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        super.onParsed(startLocation, parserDelegate);
    }
}
