package step.core.table;

import step.framework.server.tables.service.TableParameters;

import java.util.Map;

public class ActivableEntitiesTableParameters extends TableParameters {
    private boolean evaluateActivation;
    private Map<String, Object> bindings;

    public boolean isEvaluateActivation() {
        return evaluateActivation;
    }

    public void setEvaluateActivation(boolean evaluateActivation) {
        this.evaluateActivation = evaluateActivation;
    }

    public Map<String, Object> getBindings() {
        return bindings;
    }

    public void setBindings(Map<String, Object> bindings) {
        this.bindings = bindings;
    }
}
