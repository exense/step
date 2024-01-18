package step.plugins.timeseries.dashboards;

import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.plugins.timeseries.dashboards.model.DashboardView;

import java.util.stream.Stream;

public class DashboardAccessor extends AbstractAccessor<DashboardView> {
	
	public DashboardAccessor(Collection<DashboardView> collectionDriver) {
		super(collectionDriver);
	}

	public Stream<DashboardView> findLegacyDashboards() {
		return collectionDriver.find(Filters.equals("metadata.isLegacy", true), null, null, null, 0);
	}
}
