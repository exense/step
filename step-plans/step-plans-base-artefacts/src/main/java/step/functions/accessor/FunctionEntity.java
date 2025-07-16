package step.functions.accessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionLocator;
import step.core.accessors.AbstractOrganizableObject;
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
            //This is only required to recursively visit the function referenced by callFunction artefacts
            if (t instanceof CallFunction && context.isRecursive()) {
                try {
                    Function function = functionLocator.getFunction((CallFunction) t, context.getObjectFilter(),
                            null);
                    context.visitEntity(EntityManager.functions, function.getId().toString());
                } catch (NoSuchElementException e) {
                    context.getVisitor().onWarning("The keyword referenced by the call keyword artefact '" + ((CallFunction) t).getAttribute(AbstractOrganizableObject.NAME) + "' could not be found");
                }
            }
        });
    }
}
