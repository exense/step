package step.core.table;

import step.artefacts.handlers.FunctionLocator;
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.TriFunction;
import step.framework.server.Session;
import step.framework.server.tables.service.TableParameters;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ActivableEntityTableEnricher<T extends AbstractOrganizableObject> implements TriFunction<T, Session<?>, TableParameters, T> {

    public static final String ACTIVATED = "activated";

    @Override
    public T apply(T t, Session<?> session, TableParameters tableParameters) {
        //Only perform enrichment if table parameters is not null and evaluateActivation is true
        if (tableParameters instanceof ActivableEntitiesTableParameters && t instanceof AbstractOrganizableObject) {
            AbstractOrganizableObject activableEntity = (AbstractOrganizableObject) t ;
            ActivableEntitiesTableParameters parameters = (ActivableEntitiesTableParameters) tableParameters;
            if (parameters.isEvaluateActivation()) {
                //Add user from session if not yet provided in the bindings
                Map<String, Object> contextBindings = Optional.ofNullable(parameters.getBindings()).map(HashMap::new).orElse(new HashMap<>());
                if (!contextBindings.containsKey("user") && session != null) {
                    contextBindings.put("user", session.getUser().getSessionUsername());
                }
                //Test activation expression
                activableEntity.addCustomField(ACTIVATED, FunctionLocator.isActivated(contextBindings, t));
            }
        }
        return t;
    }
}
