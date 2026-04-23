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

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserDelegate;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class PatchingParserDelegate extends JsonParserDelegate {

    private final Map<JsonToken, JsonLocation> locationForToken = new HashMap<>();

    private JsonLocation lastDistinctLocation;

    protected final PatchingContext patchingContext;

    public PatchingParserDelegate(JsonParser d, PatchingContext context) {

        super(d);
        patchingContext = context;
    }

    @Override
    public JsonToken nextToken() throws IOException {
        JsonLocation preLocation = currentLocation();
        JsonToken token = super.nextToken();
        if (!preLocation.equals(currentLocation())) {
            lastDistinctLocation = preLocation;
        }
        locationForToken.put(token, preLocation);

        return token;
    }

    protected JsonLocation getLastLocationForToken(JsonToken token) {
        return locationForToken.get(token);
    }

    protected JsonLocation getLastDistinctLocation() {
        return lastDistinctLocation;
    }

    protected PatchingContext getPatchingContext() {
        return this.patchingContext;
    }
}
