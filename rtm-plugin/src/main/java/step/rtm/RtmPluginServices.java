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
		//link.link = RTMLinkGenerator.getAggregateViewByEid(executionID);
		return link;
	}	
}
