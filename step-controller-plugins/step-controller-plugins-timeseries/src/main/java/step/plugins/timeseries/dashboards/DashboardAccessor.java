package step.plugins.timeseries.dashboards;

import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.plugins.timeseries.dashboards.model.DashboardView;

public class DashboardAccessor extends AbstractAccessor<DashboardView> {
	
	public DashboardAccessor(Collection<DashboardView> collectionDriver) {
		super(collectionDriver);
	}
}
