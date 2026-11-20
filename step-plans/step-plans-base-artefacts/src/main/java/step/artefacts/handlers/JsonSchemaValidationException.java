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
package step.artefacts.handlers;

import com.fasterxml.jackson.core.JsonLocation;
import com.networknt.schema.Error;
import com.networknt.schema.path.NodePath;
import com.networknt.schema.serialization.node.TokenStreamLocationAware;
import com.networknt.schema.utils.JsonNodes;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonSchemaValidationException extends RuntimeException {


    public record JsonSchemaValidationError(int lineNumber, int columnNumber, String path, String message) {
    }

    private final List<JsonSchemaValidationError> errors;

    public JsonSchemaValidationException(List<Error> errors, Map<NodePath, JsonLocation> jsonLocationMappings) {
        this.errors = errors.stream().map(e -> {
            int lineNumber = 0;
            int columnNumber = 0;
            NodePath nodePath = e.getInstanceLocation();
            if (e.getInstanceNode() instanceof TokenStreamLocationAware) {
                JsonLocation location = JsonNodes.tokenStreamLocationOf(e.getInstanceNode());
                lineNumber = location.getLineNr();
                columnNumber = location.getColumnNr();
            } else if (jsonLocationMappings.containsKey(nodePath)) {
                JsonLocation location = jsonLocationMappings.get(nodePath);
                lineNumber = location.getLineNr();
                columnNumber = location.getColumnNr();
            }
            String path = nodePath.toString();
            if (path.isBlank()) path = "[root]";
            return new JsonSchemaValidationError(lineNumber, columnNumber, path, e.getMessage());
        }).toList();
    }

    public JsonSchemaValidationException(List<Error> errors) {
        this(errors, new HashMap<>());
    }

    public List<String> getFormattedErrorMessages() {
        return errors.stream().map(e -> MessageFormat.format(e.lineNumber() != 0 ? "Line {0}, Column {1}: {3}" : "Path {2}: {3}", e.lineNumber(), e.columnNumber(), e.path(), e.message())).toList();
    }

    public void applyJsonLocationMappings() {

    }

    public List<JsonSchemaValidationError> getValidationErrors() {
        return errors;
    }
}
