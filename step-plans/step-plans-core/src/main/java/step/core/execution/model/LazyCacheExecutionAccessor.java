package step.core.execution.model;

import step.core.accessors.Accessor;
import step.core.accessors.LazyCachedAccessor;

public class LazyCacheExecutionAccessor extends LazyCachedAccessor<Execution> {
    /**
     * @param underlyingAccessor the {@link Accessor} from which the entities should
     *                           be loaded
     */
    public LazyCacheExecutionAccessor(Accessor<Execution> underlyingAccessor) {
        super(underlyingAccessor);
    }

    public String tryResolveDescription(String id) {
        try {
            return this.get(id).getDescription();
        } catch (Exception e) {
            return UNRESOLVED;
        }
    }
}
