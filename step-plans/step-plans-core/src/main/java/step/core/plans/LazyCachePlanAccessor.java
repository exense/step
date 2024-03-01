package step.core.plans;

import step.core.accessors.Accessor;
import step.core.accessors.LazyCachedAccessor;

public class LazyCachePlanAccessor extends LazyCachedAccessor<Plan> {
    /**
     * @param underlyingAccessor the {@link Accessor} from which the entities should
     *                           be loaded
     */
    public LazyCachePlanAccessor(Accessor<Plan> underlyingAccessor) {
        super(underlyingAccessor);
    }
}
