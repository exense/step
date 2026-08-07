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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonLocation;
import step.core.yaml.PatchableYamlModel;
import step.core.yaml.PatchingContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.UnaryOperator;

public class PatchableYamlList<T> extends ArrayList<T> implements PatchableYamlModel {


    private final PatchingContext patchingContext;
    private final String fieldName;
    private volatile PatchingContext.ChunkBounds bounds;

    public PatchableYamlList(PatchingContext patchingContext, String fieldName) {

        this(new ArrayList<>(), patchingContext, fieldName);
    }

    protected PatchableYamlList(Collection<T> content, PatchingContext patchingContext, String fieldName) {
        super(content);
        this.patchingContext = Objects.requireNonNull(patchingContext);
        this.fieldName = Objects.requireNonNull(fieldName);
    }

    @Override
    @JsonIgnore
    public PatchingContext getPatchingContext() {
        return patchingContext;
    }

    @Override
    @JsonIgnore
    public String getCurrentYaml(String contextIndent) {
        if (isEmpty()) {
            return contextIndent + fieldName + ": []\n";
        }
        String childIndent = " ".repeat(contextIndent.length()) + "  - ";
        // Simply return a concatenated list of the current items; they're responsible for their own serialization
        // Note that in theory we could even try to preserve the original comments between items (if there are any),
        // but this could become complicated if entries get deleted, so we omit it for now.
        StringBuilder sb = new StringBuilder();
        sb.append(contextIndent).append(fieldName).append(":").append("\n");
        stream().map(item -> (PatchableYamlModel) item).forEach(item -> {
            sb.append(item.getCurrentYaml(childIndent));
        });
        return sb.toString();

    }

    @Override
    public boolean add(T item) {
        if (bounds == null) {
            // This list has not been registered with the context yet, meaning it hasn't been parsed from a file, but manually added.
            // We'll need to add it to the context as a new object.
            synchronized (this) {
                if (bounds == null) {
                    // FIXME: For now, this assumes all lists are top-level (see the hardcoded indent below)
                    bounds = patchingContext.appendAndClaim(this, getCurrentYaml(""));
                }
            }
        }

        PatchableYamlModel patchableItem = (PatchableYamlModel) item;
        patchableItem.setPatchingContext(this.getPatchingContext());
        patchableItem.setModified();
        super.add(item);
        return true;
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        super.replaceAll(item -> {
            T newItem = operator.apply(item);
            return newItem;
        });
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return c.stream().anyMatch(this::add);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return c.stream().anyMatch(this::remove);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return removeIf(o -> !c.contains(o));
    }

    @Override
    public void clear() {
        super.forEach(this::remove);
    }

    public void replaceItem(PatchableYamlModel oldEntity, PatchableYamlModel newEntity) {
        replaceAll(item -> item == oldEntity ? (T) newEntity : item);
        patchingContext.replaceEntity(oldEntity, newEntity);
    }

    @Override
    @JsonIgnore
    public void setPatchingContext(PatchingContext context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StartingLineDeterminationStrategy getStartingLineDeterminationStrategy() {
        return StartingLineDeterminationStrategy.NEXT_CONTENT_LINE;
    }

    @Override
    public void setModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onParsed(JsonLocation startLocation, JsonLocation endLocation) {
        bounds = patchingContext.claimChunk(startLocation, endLocation, this);
    }
}
