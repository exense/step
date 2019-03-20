package step.core.deployment;

import java.util.List;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import step.core.plugins.WebPlugin;

@Singleton
@Path("/app")
public class ApplicationServices extends AbstractServices {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/plugins")
	public List<WebPlugin> getWebPlugins() {
		return getContext().getPluginManager().getWebPlugins();
	}
}
