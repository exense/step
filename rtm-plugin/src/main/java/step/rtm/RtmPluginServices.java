package step.rtm;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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
		try {
			link.link = getAggregateViewByEid(executionID);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return link;
	}	
	
	private String getAggregateViewByEid(String eid) throws UnsupportedEncodingException {
		return "rtm/#Aggregate/select/"+URLEncoder.encode(
				"{ \"guiParams\":"
				    + "{ \"postControllerView\":"
				        + " [ { \"filters\":"
				                + " [  { \"type\":\"text\","
				                      + "\"key\":\"eId\","
				                      + "\"value\":\""+eid+"\",\"regex\":false"
		                        + " }  ]"
		                + " } ],"
	                + "\"measurementListView\":"
	                	+ " { \"nextFactor\":0,"
	                	+    "\"tableMetricChoice\":"
	                			+ "[\"begin\",\"name\",\"value\"]"
            			+ " },"
        			+ "\"aggregateSPView\":"
        				+ " { \"sessionId\":\"defaultSid\","
        				+    "\"granularity\":\"auto\","
        				+    "\"groupby\":\"name\"," 
        				+    "\"cpu\": \"1\","
        				+    "\"partition\": \"16\","
        				+    "\"timeout\": \"600\"},"
    				+ "\"aggregateGraphView\":"
        				+ " { \"chartMetricChoice\":\"AVG\"},"
    				+ "\"aggregateTableView\":"
    					+ " { \"checkedAggTableMetrics\":"
    						+ " [ \"begin\", \"AVG\", \"CNT\"]"
    						+ ",\"isSwitchedOn\":\"false\" }"
					+ "}"
				+ "}"
				, "UTF-8").replace("+", "%20");
	}
}
