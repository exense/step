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
package step.core.yaml.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ChildrenBlock;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.AbstractYamlModel;
import step.core.yaml.YamlArtefactsLookuper;
import step.core.yaml.YamlFieldCustomCopy;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.jsonschema.JsonSchema;
import step.jsonschema.JsonSchemaDefaultValueProvider;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
public abstract class AbstractYamlArtefact<T extends AbstractArtefact> extends AbstractYamlModel {

    public static final String ARTEFACT_ARRAY_DEF = "ArtefactArrayDef";

    @JsonIgnore
    protected ObjectMapper yamlObjectMapper;

    @JsonIgnore
    protected Class<T> artefactClass;

    @JsonSchema(defaultProvider = DefaultYamlArtefactNameProvider.class)
    @YamlFieldCustomCopy
    protected String nodeName;

    protected DynamicValue<Boolean> skipNode = new DynamicValue<>(false);
    protected DynamicValue<Boolean> instrumentNode = new DynamicValue<>(false);
    protected DynamicValue<Boolean> continueParentNodeExecutionOnError = new DynamicValue<>(false);
    protected String description;

    @JsonSchema(ref = YamlJsonSchemaHelper.DEFS_PREFIX + ARTEFACT_ARRAY_DEF)
    @YamlFieldCustomCopy
    protected List<NamedYamlArtefact> children = new ArrayList<>();

    @YamlFieldCustomCopy
    protected YamlChildrenBlock before;
    @YamlFieldCustomCopy
    protected YamlChildrenBlock after;

    public AbstractYamlArtefact() {
    }

    protected AbstractYamlArtefact(Class<T> artefactClass) {
        this.artefactClass = artefactClass;
    }

    public final AbstractArtefact toArtefact(){
        T artefactInstance = createArtefactInstance();
        fillArtefactFields(artefactInstance);
        return artefactInstance;
    }

    protected T createArtefactInstance(){
        try {
            return (T) artefactClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create artefact instance", e);
        }
    }

    protected void fillArtefactFields(T res) {
        res.addAttribute(AbstractOrganizableObject.NAME, getArtefactName());
        copyFieldsToObject(res, true);
        if (this.getChildren() != null) {
            for (NamedYamlArtefact child : this.getChildren()) {
                res.getChildren().add(child.getYamlArtefact().toArtefact());
            }
        }
        YamlChildrenBlock beforeYaml = this.getBefore();
        if (beforeYaml != null) {
            res.setBefore(beforeYaml.toArtefact());
        }
        YamlChildrenBlock afterYaml = this.getAfter();
        if (afterYaml != null) {
            res.setAfter(afterYaml.toArtefact());
        }
    }

    protected String getArtefactName() {
        if (getNodeName() != null) {
            // explicit name from yaml
            return getNodeName();
        } else {
            // default value
            return AbstractArtefact.getArtefactName(getArtefactClass());
        }
    }

    protected void fillYamlArtefactFields(T artefact) {
        // the node name is not obligatory in yaml - we skip this if the name is equal to the default one
        String nameAttribute = artefact.getAttribute(AbstractOrganizableObject.NAME);

        // If the name attribute is defined and doesn't equal to the default value,
        // we use it as nodeName in yaml. Otherwise, we skip this field in yaml.
        if (nameAttribute != null && !Objects.equals(nameAttribute, getDefaultNodeNameForYaml(artefact))) {
            this.setNodeName(nameAttribute);
        }
        copyFieldsFromObject(artefact, true);

        for (AbstractArtefact child : artefact.getChildren()) {
            this.getChildren().add(new NamedYamlArtefact(toYamlArtefact(child, yamlObjectMapper)));
        }

        ChildrenBlock beforeBlock = artefact.getBefore();
        if (beforeBlock != null && !beforeBlock.getSteps().isEmpty()) {
            this.setBefore(YamlChildrenBlock.toYamlChildrenBlock(beforeBlock, yamlObjectMapper));
        }

        ChildrenBlock afterBlock = artefact.getAfter();
        if (afterBlock != null && !afterBlock.getSteps().isEmpty()) {
            this.setAfter(YamlChildrenBlock.toYamlChildrenBlock(afterBlock, yamlObjectMapper));
        }
    }

    protected String getDefaultNodeNameForYaml(T artefact) {
        return AbstractArtefact.getArtefactName(artefact.getClass());
    }

    public Class<T> getArtefactClass() {
        return artefactClass;
    }

    public ObjectMapper getYamlObjectMapper() {
        return yamlObjectMapper;
    }

    public void setYamlObjectMapper(ObjectMapper yamlObjectMapper) {
        this.yamlObjectMapper = yamlObjectMapper;
    }

    public static <T extends AbstractArtefact> AbstractYamlArtefact<T> toYamlArtefact(T artefact, ObjectMapper yamlObjectMapper){
        AbstractYamlArtefact<T> instance = (AbstractYamlArtefact<T>) createYamlArtefactInstance(artefact, yamlObjectMapper);
        instance.fillYamlArtefactFields(artefact);
        return instance;
    }

    private static AbstractYamlArtefact<?> createYamlArtefactInstance(AbstractArtefact artefact, ObjectMapper yamlObjectMapper){
        Class<?> applicableYamlClass;
        if(YamlArtefactsLookuper.getSimpleYamlArtefactModels().contains(artefact.getClass())){
            applicableYamlClass = SimpleYamlArtefact.class;
        } else {
            applicableYamlClass = YamlArtefactsLookuper.getSpecialModelClassForArtefact(artefact.getClass());
        }
        try {
            if (applicableYamlClass != null) {
                AbstractYamlArtefact<?> newInstance;
                if (SimpleYamlArtefact.class.equals(applicableYamlClass)) {
                    newInstance = new SimpleYamlArtefact<>(artefact.getClass(), null, yamlObjectMapper);
                } else {
                    newInstance = (AbstractYamlArtefact<?>) applicableYamlClass.getConstructor().newInstance();
                    newInstance.setYamlObjectMapper(yamlObjectMapper);
                }
                return newInstance;
            } else {
                throw new RuntimeException("No matching yaml class found for " + artefact.getClass());
            }
        } catch (Exception ex) {
            throw new RuntimeException("Unable to create yaml artefact", ex);
        }
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public DynamicValue<Boolean> getContinueParentNodeExecutionOnError() {
        return continueParentNodeExecutionOnError;
    }

    public void setContinueParentNodeExecutionOnError(DynamicValue<Boolean> continueParentNodeExecutionOnError) {
        this.continueParentNodeExecutionOnError = continueParentNodeExecutionOnError;
    }

    public DynamicValue<Boolean> getInstrumentNode() {
        return instrumentNode;
    }

    public void setInstrumentNode(DynamicValue<Boolean> instrumentNode) {
        this.instrumentNode = instrumentNode;
    }

    public DynamicValue<Boolean> getSkipNode() {
        return skipNode;
    }

    public void setSkipNode(DynamicValue<Boolean> skipNode) {
        this.skipNode = skipNode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<NamedYamlArtefact> getChildren() {
        return children;
    }

    public void setChildren(List<NamedYamlArtefact> children) {
        this.children = children;
    }

    public YamlChildrenBlock getBefore() {
        return before;
    }

    public void setBefore(YamlChildrenBlock before) {
        this.before = before;
    }

    public YamlChildrenBlock getAfter() {
        return after;
    }

    public void setAfter(YamlChildrenBlock after) {
        this.after = after;
    }

    public static class DefaultYamlArtefactNameProvider implements JsonSchemaDefaultValueProvider {

        public DefaultYamlArtefactNameProvider() {
        }

        @Override
        public String getDefaultValue(Class<?> objectClass, Field field) {
            Class<? extends AbstractArtefact> artefactClass = YamlArtefactsLookuper.getArtefactClass(objectClass);
            return artefactClass == null ? null : AbstractArtefact.getArtefactName(artefactClass);
        }
    }

}
