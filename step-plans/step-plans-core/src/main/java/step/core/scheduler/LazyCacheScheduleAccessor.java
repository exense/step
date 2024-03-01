package step.core.scheduler;

import step.core.accessors.Accessor;
import step.core.accessors.LazyCachedAccessor;

public class LazyCacheScheduleAccessor extends LazyCachedAccessor<ExecutiontTaskParameters> {
    /**
     * @param underlyingAccessor the {@link Accessor} from which the entities should
     *                           be loaded
     */
    public LazyCacheScheduleAccessor(Accessor<ExecutiontTaskParameters> underlyingAccessor) {
        super(underlyingAccessor);
    }
}
