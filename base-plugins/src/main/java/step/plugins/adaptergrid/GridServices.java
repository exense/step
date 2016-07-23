package step.plugins.adaptergrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import step.core.artefacts.reports.ReportNode;
import step.core.deployment.AbstractServices;
import step.grid.Grid;
import step.grid.GridReportBuilder;
import step.grid.reports.TokenAssociation;
import step.grid.reports.TokenGroupCapacity;

@Path("/grid")
public class GridServices extends AbstractServices {

	private Grid getAdapterGrid() {
		return (Grid) getContext().get(GridPlugin.GRID_KEY);
	}
	
	private GridReportBuilder getReportBuilder() {
		return new GridReportBuilder(getAdapterGrid());
	}

	@GET
	@Path("/token")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TokenAssociation> getTokenAssociations() {
		return getReportBuilder().getTokenAssociations(false);
	}
	
	@GET
	@Path("/token/search")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TokenAssociation> getTokenAssociations(@QueryParam("eid") String executionID) {
		List<TokenAssociation> result = new ArrayList<>();
		for(TokenAssociation assoc:getReportBuilder().getTokenAssociations(true)) {
			Object owner = assoc.getOwner();
			if(owner!=null&&owner instanceof ReportNode && ((ReportNode)owner).getExecutionID().equals(executionID)) {
				result.add(assoc);
			}
		}
		return result;
	}
	
	@GET
	@Path("/token/usage")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TokenGroupCapacity> getUsageByIdentity(@QueryParam("groupby") List<String> groupbys) {
		return getReportBuilder().getUsageByIdentity(groupbys);
	}
	
	@GET
	@Path("/keys")
	@Produces(MediaType.APPLICATION_JSON)
	public Set<String> getTokenAttributeKeys() {
		return getReportBuilder().getTokenAttributeKeys();
	}
}
