/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.plugins.measurements.rtm;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Path("rtm")
public class RtmPluginServices {

	private static final Logger logger = LoggerFactory.getLogger(RtmPluginServices.class);
	
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
			logger.error("Error while getting rtm link for execution "+executionID, e);
		}
		return link;
	}	
	
	private String getAggregateViewByEid(String eid) throws UnsupportedEncodingException {
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
						  "}"+
						"}"
				, "UTF-8").replace("+", "%20");
	}
}
