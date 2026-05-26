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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonLocation;
import step.core.yaml.deserialization.PatchingContext;

public class PatchableYamlModelBase extends AbstractYamlModel implements PatchableYamlModel {

    @JsonIgnore
    private PatchingContext context;

    @JsonIgnore
    private int startOffset = -1;

    @JsonIgnore
    private int indent = -1;

    @JsonIgnore
    private int endOffset = -1;

    public PatchableYamlModelBase(PatchingContext context) {
        this.context = context;
    }

    @JsonIgnore
    public void setPatchingBounds(JsonLocation startLocation, JsonLocation endLocation) {
        startOffset = (int) startLocation.getCharOffset();
        endOffset = context.ensureNextEndOfLineOffset((int) endLocation.getCharOffset());
        indent = startLocation.getColumnNr() -1;
        context.getPatchables().add(this);
    }

    @JsonIgnore
    public int getStartOffset(){
        return startOffset;
    }

    @JsonIgnore
    public int getIndent() {
        return indent;
    }

    @JsonIgnore
    public int getEndOffset() {
        return endOffset;
    }

    @Override
    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    @Override
    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }


    @JsonIgnore
    @Override
    public void setIndent(int indent) {
        this.indent = indent;
    }

    @Override
    public void setContext(PatchingContext context) {
        this.context = context;
    }
}
