package step.plugins.screentemplating;

import step.core.objectenricher.ObjectPredicate;
import step.functions.Function;

import java.util.HashMap;
import java.util.Map;

public class FunctionTableScreenInputs {

    public static final String ATTRIBUTES = "attributes.";
    public static final String FUNCTION_TABLE = "functionTable";
    private final ScreenTemplateManager screenTemplateManager;

    public FunctionTableScreenInputs(ScreenTemplateManager screenTemplateManager) {
        this.screenTemplateManager = screenTemplateManager;
    }

    public Map<String, String> getSelectionAttributes(Function function, Map<String, Object> contextBindings, ObjectPredicate objectPredicae) {
        HashMap<String, String> attributes = new HashMap<>();
        screenTemplateManager.getInputsForScreen(FUNCTION_TABLE, contextBindings, objectPredicae).forEach(i -> {
            String inputId = i.getId();
            if (inputId.startsWith(ATTRIBUTES)) {
                String attributeKey = inputId.replace(ATTRIBUTES, "");
                String attributeValue = function.getAttribute(attributeKey);
                if (attributeValue != null) {
                    attributes.put(attributeKey, attributeValue);
                }
            }
        });
        return attributes;
    }
}
