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
package step.core.yaml.deserialization;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonNode;
import step.core.yaml.PatchableYamlModelBase;
import step.core.yaml.PatchingContext;

import java.util.Objects;

public class PatchableYamlPrimitive<T> extends PatchableYamlModelBase {
    @JsonIgnore
    protected T value;


    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public PatchableYamlPrimitive(@JacksonInject(useInput = OptBoolean.FALSE) PatchingContext context, T value) {
        super(context);
        this.value = value;
    }

    @Override
    public String toString() {
        return value != null ? value.toString() : "";
    }

    public T getValue() {
        return value;
    }

    @JsonValue
    public JsonNode getJsonValue() {
        return getPatchingContext().getMapper().valueToTree(value);
    }

    public void setValue(T value) {
        this.value = value;
        setModified();

        PatchingContext context = getPatchingContext();
        if (!context.chunkClaimed(this)) {
            context.appendAndClaim(this, getCurrentYaml(""), PatchingContext.ChunkBounds.Portion.HEAD);
        }
    }

    @Override
    public void onParsed(JsonLocation startLocation, PatchingParserDelegate parserDelegate) {
        getPatchingContext().claimChunk(startLocation, startLocation, this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PatchableYamlPrimitive<?> that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
