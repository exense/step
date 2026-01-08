package step.functions.accessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionLocator;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.entities.Entity;
import step.core.entities.EntityConstants;
import step.core.entities.EntityManager;
import step.functions.Function;

import java.util.NoSuchElementException;

public class FunctionEntity extends Entity<Function, Accessor<Function>> {

    private static final Logger logger = LoggerFactory.getLogger(FunctionEntity.class);

    public FunctionEntity(Accessor<Function> accessor, FunctionLocator functionLocator, EntityManager entityManager) {
        super(EntityConstants.functions, accessor, Function.class);
        entityManager.addDependencyTreeVisitorHook((t, context) -> {
            //Only apply logic is the entity is a CallFunction
            if (t instanceof CallFunction) {
                CallFunction callFunction = (CallFunction) t;
                switch (context.getVisitMode()) {
                    case RECURSIVE:
                        //In recursive mode we visit the resolved entity recursively (the highest priority is chosen if multiple entity matches)
                        try {
                            Function function = functionLocator.getFunction(callFunction, context.getObjectPredicate(),
                                    null);
                            context.visitEntity(EntityConstants.functions, function.getId().toString());
                        } catch (NoSuchElementException e) {
                            context.getVisitor().onWarning("The keyword referenced by the call keyword artefact '" + (callFunction).getAttribute(AbstractOrganizableObject.NAME) + "' could not be found");
                        }
                        break;
                    case RESOLVE_ALL:
                        //In resolve All mode we resolve all matching entities but do not visit recursively
                        functionLocator.getMatchingFunctions(callFunction, context.getObjectPredicate(), null).forEach(f -> {
                            context.onResolvedEntity(EntityConstants.functions, f.getId().toHexString(), f);
                        });
                        break;
                }
            }
        });
    }
}
