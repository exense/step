package step.core.table;

import step.artefacts.handlers.LocatorHelper;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.AbstractUser;
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
        if (tableParameters instanceof ActivableEntitiesTableParameters && t != null) {
            ActivableEntitiesTableParameters parameters = (ActivableEntitiesTableParameters) tableParameters;
            if (parameters.isEvaluateActivation()) {
                //Add user from session if not yet provided in the bindings
                Map<String, Object> contextBindings = enrichBindingsWithSession(session, parameters.getBindings());
                //Test activation expression
                t.addCustomField(ACTIVATED, LocatorHelper.isActivated(contextBindings, t));
            }
        }
        return t;
    }

    public static Map<String, Object> enrichBindingsWithSession(Session<?> session, Map<String, Object> bindings) {
        //Add the user from the session, if present, to the bindings
        Optional<String> optionalUserName = Optional.ofNullable(session).map(Session::getUser).map(AbstractUser::getSessionUsername);
        if (optionalUserName.isPresent()) {
            Map<String, Object> contextBindings = Optional.ofNullable(bindings).map(HashMap::new).orElse(new HashMap<>());
            contextBindings.put("user", optionalUserName.get());
            return contextBindings;
        } else {
            return bindings;
        }
    }
}
