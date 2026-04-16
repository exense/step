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

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.UnaryOperator;

public class PatchableYamlList<T> extends ArrayList<T> implements PatchableYamlModel{


    private PatchingContext context;
    private final String fieldName;

    @JsonIgnore
    private int startOffset = -1;

    @JsonIgnore
    private int indent = -1;

    @JsonIgnore
    private int endOffset = -1;

    public PatchableYamlList(PatchingContext context, String fieldName) {

        this(new ArrayList<>(), context, fieldName);
    }

    protected PatchableYamlList(Collection<T> delegate, PatchingContext context, String fieldName) {
        super(delegate);
        this.context = context;
        this.fieldName = fieldName;
    }

    @Override
    public boolean remove(Object item) {
        if (super.remove(item)) {
            PatchableYamlModel patchableItem = (PatchableYamlModel) item;
            context.removePatchable(patchableItem);

            if (super.isEmpty()) {
                context.removePatchable(this);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean add(T item) {
        if (!context.contains(this)) {
            context.appendEmptyPatchable(this);
        }
        PatchableYamlModel patchableItem = (PatchableYamlModel)  item;

        if (super.isEmpty()) {
            patchableItem.setIndent(getListItemMarker().length());
            context.replaceContainerPatchable(this, patchableItem, fieldName + ":\n" + getListItemMarker());
        } else {
            PatchableYamlModel last = (PatchableYamlModel) get(size()-1);

            context.addPatchableAfter(last, patchableItem, getListItemMarker(last));
        }
        super.add(item);
        return true;
    }

    private String getListItemMarker(PatchableYamlModel last) {
        String yaml = context.getYaml();
        int listItemMarkerStartOffset = yaml.lastIndexOf("\n", last.getStartOffset());
        return yaml.substring( listItemMarkerStartOffset, last.getStartOffset());
    }

    private String getListItemMarker() {
        return " ".repeat(indent) + "- ";
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

    public void  replaceItem(PatchableYamlModel oldEntity, PatchableYamlModel newEntity) {
        replaceAll(item -> item == oldEntity ? (T) newEntity : item);
        context.replacePatchable(oldEntity, newEntity);
    }

    @JsonIgnore
    public void setPatchingBounds(JsonLocation startLocation, JsonLocation endLocation) {
        startOffset = (int) startLocation.getCharOffset();
        endOffset = context.ensureNextEndOfLineOffset((int) endLocation.getCharOffset());
        indent = startLocation.getColumnNr() -1;
        context.getPatchables().add(this);
    }

    @Override
    public int getStartOffset(){
        return startOffset;
    }

    @Override
    public int getIndent() {
        return indent;
    }

    @Override
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
    @JsonIgnore
    public void setContext(PatchingContext context) {
        this.context = context;
    }
}
