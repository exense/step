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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import step.core.yaml.PatchableYamlModel;

import java.util.ArrayList;
import java.util.List;

public class PatchingContext {
    private String yaml;

    private final List<PatchableYamlModel> patchables = new ArrayList<>();
    private final ObjectMapper mapper;

    public PatchingContext() {
        yaml = "";
        mapper = new ObjectMapper();
    }

    public PatchingContext(String yaml, ObjectMapper mapper) {
        this.yaml = yaml;
        this.mapper = mapper;
    }

    public String getYaml() {
        return yaml;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public List<PatchableYamlModel> getPatchables() {
        return patchables;
    }

    public void setYaml(String yaml) {
        this.yaml = yaml;
    }

    private String entityStringWithIndent(Object entity, int indent) {
        try {
            String indentString = " ".repeat(indent);
            return mapper
                .writeValueAsString(entity)
                .replaceAll("---\n", "")
                .trim()
                .replaceAll("\n", "\n" + indentString);
        } catch (JsonProcessingException e) {
            throw new AutomationPackageUpdateException("Error Serializing new object", e);
        }
    }

    public void replacePatchable(PatchableYamlModel oldPatchable, PatchableYamlModel newPatchable) {
        String entityString = entityStringWithIndent(newPatchable, oldPatchable.getIndent());

        int endOffset = oldPatchable.getEndOffset();
        int delta = entityString.length() - (endOffset - oldPatchable.getStartOffset());


        yaml = yaml.substring(0, oldPatchable.getStartOffset())
            + entityString
            + yaml.substring(oldPatchable.getEndOffset());

        newPatchable.setStartOffset(oldPatchable.getStartOffset());
        newPatchable.setIndent(oldPatchable.getIndent());
        newPatchable.setEndOffset(oldPatchable.getEndOffset() + delta);

        patchables.replaceAll(p -> p == oldPatchable ? newPatchable : p);

        updatePatchableOffsetsAfter(newPatchable, endOffset, delta);
    }

    public void removePatchable(PatchableYamlModel patchable) {
        if (!patchables.contains(patchable)) return;

        int previousLineEnd = ensurePreviousEndOfLineOffset(patchable.getStartOffset());
        int delta = previousLineEnd - patchable.getEndOffset();
        yaml = yaml.substring(0, previousLineEnd) + yaml.substring(patchable.getEndOffset());
        updatePatchableOffsetsAfter(patchable, patchable.getEndOffset(), delta);
        patchables.remove(patchable);
    }



    public void addPatchableAfter(PatchableYamlModel last, PatchableYamlModel patchableItem, String entityPrefix) {

        String entityString = entityStringWithIndent(patchableItem, last.getIndent());

        yaml = yaml.substring(0, last.getEndOffset()) + entityPrefix + entityString + yaml.substring(last.getEndOffset());

        patchableItem.setStartOffset(last.getEndOffset() + entityPrefix.length());
        patchableItem.setEndOffset(last.getEndOffset() + entityPrefix.length() + entityString.length());
        patchableItem.setIndent(last.getIndent());
        patchableItem.setContext(this);

        patchables.add(patchables.indexOf(last)+1, patchableItem);

        updatePatchableOffsetsAfter(patchableItem, patchableItem.getEndOffset(), entityString.length() + entityPrefix.length());
    }

    private void updatePatchableOffsetsAfter(PatchableYamlModel patchable, int endOffset, int delta) {
        for(int i = patchables.indexOf(patchable) + 1; i < patchables.size(); i++) {
            PatchableYamlModel successor = patchables.get(i);
            if (successor.getStartOffset() >= endOffset) {
                successor.setStartOffset(successor.getStartOffset()+delta);
            }
            successor.setEndOffset(successor.getEndOffset()+delta);
        }
    }

    public boolean contains(PatchableYamlModel patchable) {
        return patchables.contains(patchable);
    }

    public int ensureNextEndOfLineOffset(int offset) {
        return Math.max(yaml.indexOf("\n", offset), offset);
    }

    public int ensurePreviousEndOfLineOffset(int offset) {
        return Math.max(yaml.lastIndexOf("\n", offset), 0);
    }

    public void replaceContainerPatchable(PatchableYamlModel containerPatchable, PatchableYamlModel child, String containerPrefix) {
        String childString = entityStringWithIndent(child, child.getIndent());
        yaml = yaml.substring(0, containerPatchable.getStartOffset()) + containerPrefix + childString + yaml.substring(containerPatchable.getEndOffset());

        containerPatchable.setEndOffset(containerPatchable.getStartOffset() + containerPrefix.length() + childString.length());
        child.setStartOffset(containerPatchable.getStartOffset() + containerPrefix.length());
        child.setEndOffset(containerPatchable.getEndOffset());

        patchables.add(patchables.indexOf(containerPatchable), child);

        int delta = containerPrefix.length() + childString.length() - (containerPatchable.getEndOffset() - containerPatchable.getStartOffset());
        updatePatchableOffsetsAfter(containerPatchable, containerPatchable.getEndOffset(), delta);
    }

    public void appendEmptyPatchable(PatchableYamlModel patchable) {
        patchable.setIndent(0);
        yaml += "\n";
        patchable.setStartOffset(yaml.length());
        patchable.setEndOffset(yaml.length());
        patchable.setIndent(0);
        patchables.add(patchable);
    }
}
