package step.core.artefacts;

import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.jsonschema.JsonSchema;

import java.util.ArrayList;
import java.util.List;

import static step.core.yaml.model.AbstractYamlArtefact.ARTEFACT_ARRAY_DEF;

public class ChildrenBlock {

    @JsonSchema(ref = YamlJsonSchemaHelper.DEFS_PREFIX + ARTEFACT_ARRAY_DEF)
    protected List<AbstractArtefact> steps = new ArrayList<>();

    protected DynamicValue<Boolean> continueOnError = new DynamicValue<Boolean>(false);

    public void setSteps(List<AbstractArtefact> steps) {
        this.steps = steps;
    }

    public boolean addStep(AbstractArtefact e) {
        return steps.add(e);
    }

    @EntityReference(type= EntityManager.recursive)
    public List<AbstractArtefact> getSteps() {
        return steps;
    }

    public DynamicValue<Boolean> getContinueOnError() {
        return continueOnError;
    }

    public void setContinueOnError(DynamicValue<Boolean> continueOnError) {
        this.continueOnError = continueOnError;
    }
}
