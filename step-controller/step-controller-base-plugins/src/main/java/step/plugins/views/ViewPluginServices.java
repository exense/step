package step.plugins.views;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import step.core.deployment.AbstractServices;

@Singleton
@Path("/views")
public class ViewPluginServices extends AbstractServices {
	
	ViewPlugin plugin;

	public ViewPluginServices() {
		super();
	}
	
	@PostConstruct
	public void init() throws Exception {	
		super.init();
		plugin = (ViewPlugin) controller.getContext().get(ViewPlugin.VIEW_PLUGIN_KEY);
	}

	@GET
	@Path("/{id}/{executionId}")
	@Produces(MediaType.APPLICATION_JSON)
	public ViewModel getView(@PathParam("id") String viewId, @PathParam("executionId") String executionId) {
		return plugin.query(viewId, executionId);
		
	}
}
