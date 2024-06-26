package step.plugins.timeseries;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import step.controller.services.entities.AbstractEntityServices;
import step.core.GlobalContext;
import step.core.entities.EntityManager;
import step.framework.server.security.Secured;
import step.framework.server.security.SecuredContext;
import step.plugins.timeseries.dashboards.DashboardAccessor;
import step.plugins.timeseries.dashboards.model.DashboardView;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static step.plugins.timeseries.TimeSeriesControllerPlugin.GENERATION_NAME;


@Singleton
@Path("/dashboards")
@Tag(name = "Dashboards")
@Tag(name = "Entity=Dashboard")
@SecuredContext(key = "entity", value = "dashboard")
public class DashboardsService extends AbstractEntityServices<DashboardView> { // AbstractEntityServices + init table

	private DashboardAccessor accessor;

	public DashboardsService() {
		super(EntityManager.dashboards);
	}

	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		accessor = context.require(DashboardAccessor.class);
	}
	
	@Secured(right = "dashboard-read")
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public List<DashboardView> getAll() {
		 return accessor.stream().collect(Collectors.toList());
	}


	@Override
	public DashboardView clone(String id) {
		DashboardView clone = super.clone(id);
		Map<String, Object> customFields = clone.getCustomFields();
		if (customFields != null) {
			customFields.remove(GENERATION_NAME);
		}
		save(clone);
		return  clone;
	}
}
