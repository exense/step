package step.plugins.timeseries;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import step.core.GlobalContext;
import step.core.deployment.AbstractStepServices;
import step.framework.server.security.Secured;
import step.plugins.timeseries.dashboards.DashboardAccessor;
import step.plugins.timeseries.dashboards.model.DashboardView;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
@Path("/dashboards")
@Tag(name = "Dashboards")
public class DashboardsService extends AbstractStepServices {

	private DashboardAccessor accessor;
	
	@PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        accessor = context.require(DashboardAccessor.class);
    }
	
	@Secured(right = "execution-read")
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public List<DashboardView> getAll() {
		 return accessor.stream().collect(Collectors.toList());
	}
	
	public void save() {
		
	}
	
	public void update() {
		
	}

}
