package step.rtm;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
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
	public RTMLink getRtmLink(ContainerRequestContext requestContext, @PathParam("id") String executionID) {
		RTMLink link = new RTMLink();
		Cookie sessionCookie = requestContext.getCookies().get("sessionid");
		try {
			link.link = getAggregateViewByEid(sessionCookie.getValue(), executionID);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return link;
	}	
	
	private String getAggregateViewByEid(String cookie, String eid) throws UnsupportedEncodingException {
		return "rtm/#Aggregate/select/"+URLEncoder.encode(
				"{"+
						  "\"guiParams\": {"+
						    "\"postControllerView\": {"+
						      "\"selectors1\": ["+
						        "{"+
						          "\"filters\": ["+
						            "{"+
						              "\"type\": \"text\","+
						              "\"key\": \"eId\","+
						              "\"value\": \""+eid+"\","+
						              "\"regex\": \"\""+
						            "}"+
						          "]"+
						        "}"+
						      "]"+
						    "},"+
						    "\"measurementListView\": {"+
						      "\"nextFactor\": \"0\","+
						      "\"tableMetricChoice\": ["+
						        "\"begin\","+
						        "\"name\","+
						        "\"value\""+
						      "]"+
						    "},"+
						    "\"aggregateSPView\": {"+
						      "\"sessionId\": \"defaultSid\","+
						      "\"granularity\": \"auto\","+
						      "\"groupby\": \"name\","+
						      "\"cpu\": \"1\","+
						      "\"partition\": \"8\","+
						      "\"timeout\": \"600\""+
						    "},"+
						    "\"aggregateGraphView\": {"+
						      "\"chartMetricChoice\": \"avg\""+
						    "},"+
						    "\"aggregateTableView\": {"+
						      "\"checkedAggTableMetrics\": ["+
						        "\"begin\","+
						        "\"cnt\","+
						        "\"avg\""+
						      "],"+
						      "\"isSwitchedOn\": \"false\""+
						    "}"+
						  "},"+
						   "\"sessionToken\" : \""+cookie+"\""+
						"}"
				, "UTF-8").replace("+", "%20");
	}
}
