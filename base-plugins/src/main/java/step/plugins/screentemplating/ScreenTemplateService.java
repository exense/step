package step.plugins.screentemplating;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import step.core.deployment.AbstractServices;

@Singleton
@Path("screens")
public class ScreenTemplateService extends AbstractServices {
	
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Input> getInputsForScreen(@PathParam("id") String screenId, @Context UriInfo uriInfo) {
		Map<String, Object> contextBindings = getContextBindings(uriInfo);
		ScreenTemplatePlugin plugin = (ScreenTemplatePlugin) getContext().get(ScreenTemplatePlugin.SCREEN_TEMPLATE_KEY);
		return plugin.getInputsForScreen(screenId, contextBindings);
	}

	private Map<String, Object> getContextBindings(UriInfo uriInfo) {
		Map<String, Object> contextBindings = new HashMap<>();
		for(String key:uriInfo.getQueryParameters().keySet()) {
			contextBindings.put(key, uriInfo.getQueryParameters().getFirst(key));
		}
		return contextBindings;
	}

}
