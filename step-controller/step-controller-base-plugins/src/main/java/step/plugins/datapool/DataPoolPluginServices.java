package step.plugins.datapool;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.datapool.DataPoolConfiguration;
import step.datapool.DataPoolFactory;

@Singleton
@Path("/datapool")
public class DataPoolPluginServices extends AbstractServices {
	
	@GET
	@Path("/types/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public DataPoolConfiguration getDataPoolDefaultInstance(@PathParam("id") String type) throws Exception {
		return DataPoolFactory.getDefaultDataPoolConfiguration(type);
	}
}
