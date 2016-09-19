package step.rtm;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Singleton
@Path("rtm")
public class RtmPluginServices {

	
	public class RTMLink {
		String link;
		public String getLink() {
			return link;
		}
	}
	
	@GET
	@Path("/rtmlink/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public RTMLink getRtmLink(@PathParam("id") String executionID) {
		RTMLink link = new RTMLink();
		link.link = getAggregateViewByEid(executionID);
		return link;
	}	
	
	private String getAggregateViewByEid(String eid) {
		return "rtm/#Aggregate/select/%7B%22guiParams%22%3A%7B%22postControllerView%22%3A%5B%7B%22filters%22%3A%5B%7B%22type%22%3A%22text%22%2C%22key%22%3A%22eid%22%2C%22value%22%3A%22"+
				eid+"%22%2C%22regex%22%3Afalse%7D%5D%7D%5D%2C%22measurementListView%22%3A%7B%22nextFactor%22%3A0%2C%22tableMetricChoice%22%3A%5B%22begin%22%2C%22name%22%2C%22value%22%5D%7D%2C%22aggregateSPView%22%3A%7B%22sessionId%22%3A%22defaultSid%22%2C%22granularity%22%3A%22900000%22%2C%22groupby%22%3A%22name%22%7D%2C%22aggregateGraphView%22%3A%7B%22chartMetricChoice%22%3A%22avg%22%7D%2C%22aggregateTableView%22%3A%7B%22checkedAggTableMetrics%22%3A%5B%22begin%22%2C%22avg%22%2C%22cnt%22%5D%2C%22isSwitchedOn%22%3A%22false%22%7D%7D%7D/1471591887164";
	}
}
