package step.functions.accessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionLocator;
import step.core.accessors.Accessor;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.functions.Function;

import java.util.NoSuchElementException;

public class FunctionEntity extends Entity<Function, Accessor<Function>> {

    private static final Logger logger = LoggerFactory.getLogger(FunctionEntity.class);

    public FunctionEntity(Accessor<Function> accessor, FunctionLocator functionLocator, EntityManager entityManager) {
        super(EntityManager.functions, accessor, Function.class);
        entityManager.addDependencyTreeVisitorHook((t, context) -> {
            if (t instanceof CallFunction) {
                try {
                    Function function = functionLocator.getFunction((CallFunction) t, context.getObjectPredicate(),
                            null);
                    if (function != null) {
                        context.visitEntity(EntityManager.functions, function.getId().toString());
                    }
                } catch (NoSuchElementException e) {
                    if(logger.isDebugEnabled()) {
                        logger.debug("The function referenced by the artefact " + t + " could not be found", e);
                    }
                }
            }
        });
    }
}
