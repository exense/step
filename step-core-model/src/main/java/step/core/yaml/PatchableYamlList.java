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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PatchableYamlList<T> extends ArrayList<T> implements PatchableYamlModel {

    @JsonIgnore
    private int startOffset = -1;

    @JsonIgnore
    private int startColumn = -1;

    @JsonIgnore
    private int endOffset = -1;

    @JsonIgnore
    private int startFieldOffset;

    public PatchableYamlList() {
        super();
    }

    public PatchableYamlList(Collection<T> delegate) {
        super(delegate);
    }

    @JsonIgnore
    public void setPatchingBounds(JsonLocation startLocation, int startFieldLocation, JsonLocation endLocation) {
        startFieldOffset = startFieldLocation;
        startOffset = (int) startLocation.getCharOffset();
        endOffset = (int) endLocation.getCharOffset();
        startColumn = startLocation.getColumnNr() -1;
    }

    @JsonIgnore
    public int getStartOffset(){
        return startOffset;
    }

    @JsonIgnore
    public int getIndent() {
        return startColumn;
    }

    @JsonIgnore
    public int getEndOffset() {
        return endOffset;
    }

    @JsonIgnore
    public void setPatchingBounds(PatchableYamlModel newBoundedArtefact) {
        startFieldOffset = newBoundedArtefact.getStartFieldOffset();
        startOffset = newBoundedArtefact.getStartOffset();
        startColumn = newBoundedArtefact.getIndent();
        endOffset = newBoundedArtefact.getEndOffset();
    }

    @Override
    public int getStartFieldOffset() {
        return startFieldOffset;
    }
}
