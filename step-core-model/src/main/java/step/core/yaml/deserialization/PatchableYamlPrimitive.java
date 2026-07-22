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
import step.core.yaml.PatchableYamlModelBase;
import step.core.yaml.PatchingContext;

import java.util.Objects;

public class PatchableYamlPrimitive<T> extends PatchableYamlModelBase {
    @JsonIgnore
    private T value;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public PatchableYamlPrimitive(@JacksonInject(useInput = OptBoolean.FALSE) PatchingContext context, T value) {
        super(context);
        this.value = value;
        Objects.requireNonNull(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @JsonValue
    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public void onParsed(JsonLocation startLocation, JsonLocation endLocation) {
        // FIXME: Yes, we use startLocation twice here. This is a workaround for a known bug, see SED-4847
        getPatchingContext().claimChunk(startLocation, startLocation, this);
    }
}
