package step.core.yaml.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import step.core.artefacts.ChildrenBlock;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.jsonschema.JsonSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static step.core.yaml.model.AbstractYamlArtefact.ARTEFACT_ARRAY_DEF;
import static step.core.yaml.model.AbstractYamlArtefact.toYamlArtefact;

public class YamlChildrenBlock {
    protected DynamicValue<Boolean> continueOnError = new DynamicValue<Boolean>(false);

    @JsonSchema(ref = YamlJsonSchemaHelper.DEFS_PREFIX + ARTEFACT_ARRAY_DEF)
    protected List<NamedYamlArtefact> steps = new ArrayList<>();

    public YamlChildrenBlock() {
    }

    public DynamicValue<Boolean> getContinueOnError() {
        return continueOnError;
    }

    public void setContinueOnError(DynamicValue<Boolean> continueOnError) {
        this.continueOnError = continueOnError;
    }

    public List<NamedYamlArtefact> getSteps() {
        return steps;
    }

    public void setSteps(List<NamedYamlArtefact> steps) {
        this.steps = steps;
    }

    public static YamlChildrenBlock toYamlChildrenBlock(ChildrenBlock childrenBlock, ObjectMapper yamlObjectMapper) {
        YamlChildrenBlock yamlChildrenBlock = new YamlChildrenBlock();
        yamlChildrenBlock.setContinueOnError(childrenBlock.getContinueOnError());
        yamlChildrenBlock.setSteps(childrenBlock.getSteps().stream().map(b -> new NamedYamlArtefact(toYamlArtefact(b, yamlObjectMapper))).collect(Collectors.toList()));
        return yamlChildrenBlock;
    }

    public ChildrenBlock toArtefact() {
        ChildrenBlock childrenBlock = new ChildrenBlock();
        childrenBlock.setContinueOnError(this.getContinueOnError());
        childrenBlock.setSteps(this.getSteps().stream().map(s -> s.getYamlArtefact().toArtefact()).collect(Collectors.toList()));
        return childrenBlock;
    }
}
