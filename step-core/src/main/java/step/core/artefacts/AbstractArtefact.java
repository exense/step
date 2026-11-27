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
package step.core.artefacts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import org.bson.types.ObjectId;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.MapDeserializer;
import step.core.accessors.MapSerializer;
import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityConstants;
import step.core.entities.EntityReference;
import step.core.yaml.YamlFieldOrder;
import step.core.yaml.YamlFieldPriority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonTypeInfo(use = Id.CUSTOM, property = AbstractArtefact.JSON_CLASS_PROPERTY)
@JsonTypeIdResolver(ArtefactTypeIdResolver.class)
public abstract class AbstractArtefact extends AbstractOrganizableObject {

    public static final String JSON_CLASS_PROPERTY = "_class";

    @YamlFieldOrder(YamlFieldPriority.DESCRIPTORS)
    protected DynamicValue<String> dynamicName;

    @YamlFieldOrder(YamlFieldPriority.DESCRIPTORS)
    protected boolean useDynamicName;

    @YamlFieldOrder(YamlFieldPriority.DESCRIPTORS)
    protected String description;


    @JsonSerialize(using = MapSerializer.class)
    @JsonDeserialize(using = MapDeserializer.class)
    @YamlFieldOrder(YamlFieldPriority.DESCRIPTORS)
    protected Map<String, Object> customAttributes;

    protected List<ObjectId> attachments;

    @YamlFieldOrder(YamlFieldPriority.EXECUTION_CONTROL)
    private DynamicValue<Boolean> skipNode = new DynamicValue<>(false);
    @YamlFieldOrder(YamlFieldPriority.EXECUTION_CONTROL)
    private DynamicValue<Boolean> instrumentNode = new DynamicValue<>(false);
    @YamlFieldOrder(YamlFieldPriority.EXECUTION_CONTROL)
    private DynamicValue<Boolean> continueParentNodeExecutionOnError = new DynamicValue<>(false);
    private boolean isWorkArtefact = false;


    @YamlFieldOrder(YamlFieldPriority.BEFORE)
    private ChildrenBlock before;

    @YamlFieldOrder(YamlFieldPriority.CHILDREN)
    protected List<AbstractArtefact> children = new ArrayList<>();

    @YamlFieldOrder(YamlFieldPriority.AFTER)
    private ChildrenBlock after;

    public AbstractArtefact() {
        super();
        Map<String, String> defaultAttributes = new HashMap<>();
        defaultAttributes.put("name", getArtefactName(this.getClass()));
        attributes = defaultAttributes;
        dynamicName = new DynamicValue<String>("");
        //dynamicName.setDynamic(true);
        dynamicName.setExpression("");
    }

    public static String getArtefactName(Class<? extends AbstractArtefact> artefactClass) {
        Artefact annotation = artefactClass.getAnnotation(Artefact.class);
        return !annotation.name().isEmpty() ? annotation.name() : artefactClass.getSimpleName();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @EntityReference(type = EntityConstants.recursive)
    public List<AbstractArtefact> getChildren() {
        return children;
    }

    public void setChildren(List<AbstractArtefact> children) {
        this.children = children;
    }

    public boolean addChild(AbstractArtefact e) {
        return children.add(e);
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, Object> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public Object getCustomAttribute(String key) {
        if (customAttributes != null) {
            return customAttributes.get(key);
        } else {
            return null;
        }
    }

    public synchronized void addCustomAttribute(String key, Object value) {
        if (customAttributes == null) {
            customAttributes = new HashMap<>();
        }
        customAttributes.put(key, value);
    }

    public void addAttachment(ObjectId attachmentId) {
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        attachments.add(attachmentId);
    }

    public void setAttachments(List<ObjectId> attachments) {
        this.attachments = attachments;
    }

    public List<ObjectId> getAttachments() {
        return attachments;
    }

    @JsonIgnore
    public boolean isCreateSkeleton() {
        return false;
    }

    /**
     * @return true if this Artefact is calling Artefacts from other plans
     */
    @JsonIgnore
    public boolean isCallingArtefactsFromOtherPlans() {
        return false;
    }

    /**
     * @return
     * @deprecated This field has been deprecated and isn't used anymore.
     * The getter and setter have been kept in the model to avoid deserialization issues
     * TODO implement a migration task and remove the getter and setter
     */
    @JsonIgnore
    @Deprecated
    public boolean isPersistNode() {
        return true;
    }

    /**
     * @param persistNode
     * @deprecated This field has been deprecated and isn't used anymore.
     * The setter has been kept in the model to avoid deserialization issues
     */
    @Deprecated
    public void setPersistNode(boolean persistNode) {
    }

    public DynamicValue<Boolean> getSkipNode() {
        return skipNode;
    }

    public void setSkipNode(DynamicValue<Boolean> skipNode) {
        this.skipNode = skipNode;
    }

    public DynamicValue<String> getDynamicName() {
        return dynamicName;
    }

    public void setDynamicName(DynamicValue<String> dynamicName) {
        this.dynamicName = dynamicName;
    }

    public boolean isUseDynamicName() {
        return useDynamicName;
    }

    public void setUseDynamicName(boolean useDynamicName) {
        this.useDynamicName = useDynamicName;
    }

    /**
     * Returns the direct children of all the children slots of this artefact, in execution order: the steps of
     * the before block, the main children and the steps of the after block. Artefacts defining additional
     * {@link ChildrenBlock} add them by overriding {@link #collectMainChildren(List)}.
     * <p>
     * Use this method, and not {@link #getChildren()}, to walk the whole artefact tree.
     *
     * @return a new list holding the direct children of all slots
     */
    public List<AbstractArtefact> allChildren() {
        List<AbstractArtefact> result = new ArrayList<>();
        addSteps(result, getBefore());
        collectMainChildren(result);
        addSteps(result, getAfter());
        return result;
    }

    /**
     * Adds the children executed between the before and the after blocks. These are the main children,
     * plus the artefacts of any additional {@link ChildrenBlock} the artefact defines.
     *
     * @param result the list to add the children to
     */
    protected void collectMainChildren(List<AbstractArtefact> result) {
        List<AbstractArtefact> children = getChildren();
        if (children != null) {
            result.addAll(children);
        }
    }

    protected static void addSteps(List<AbstractArtefact> result, ChildrenBlock block) {
        if (block != null && block.getSteps() != null) {
            result.addAll(block.getSteps());
        }
    }

    public void deepCleanupAllCustomAttributes() {
        if (getCustomAttributes() != null) {
            getCustomAttributes().clear();
        }
        allChildren().forEach(AbstractArtefact::deepCleanupAllCustomAttributes);
    }

    /**
     * Assigns a new id to this artefact and to all its descendants, as done when cloning a plan or an artefact
     */
    public void deepAssignNewIds() {
        setId(new ObjectId());
        allChildren().forEach(AbstractArtefact::deepAssignNewIds);
    }

    public DynamicValue<Boolean> getInstrumentNode() {
        return instrumentNode;
    }

    public void setInstrumentNode(DynamicValue<Boolean> instrumentNode) {
        this.instrumentNode = instrumentNode;
    }

    public DynamicValue<Boolean> getContinueParentNodeExecutionOnError() {
        return continueParentNodeExecutionOnError;
    }

    public void setContinueParentNodeExecutionOnError(DynamicValue<Boolean> continueOnError) {
        this.continueParentNodeExecutionOnError = continueOnError;
    }

    public boolean isWorkArtefact() {
        return isWorkArtefact;
    }

    public void setWorkArtefact(boolean workArtefact) {
        isWorkArtefact = workArtefact;
    }

    @EntityReference(type = EntityConstants.recursive)
    public ChildrenBlock getBefore() {
        return before;
    }

    public void setBefore(ChildrenBlock before) {
        this.before = before;
    }

    @EntityReference(type = EntityConstants.recursive)
    public ChildrenBlock getAfter() {
        return after;
    }

    public void setAfter(ChildrenBlock after) {
        this.after = after;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractArtefact other = (AbstractArtefact) obj;
        if (getId() == null) {
            if (other.getId() != null)
                return false;
        } else if (!getId().equals(other.getId()))
            return false;
        return true;
    }

    /**
     * Void class to be used in annotations instead of null-values
     */
    public static final class None extends AbstractArtefact {
    }
}
