/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.plugins.events;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import step.core.deployment.AbstractServices;

@Singleton
@Path("/eventbroker")
public class EventBrokerServices extends AbstractServices {

	private EventBroker eb;

	public EventBrokerServices(){
		super();
	}

	@PostConstruct
	public void init() {
		eb = (EventBroker) getContext().get(EventBroker.class);
	}

	@GET
	@Path("/events")
	@Produces(MediaType.APPLICATION_JSON)
	//@Consumes(MediaType.APPLICATION_JSON)
	public Map<String, Event> getEventBrokerStatus() {
		return eb.asMap();
	}

	@POST
	@Path("/event")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Event putEvent(Event event) throws Exception {
		if(event != null)
			event.setSubmitionTimestamp(System.currentTimeMillis());

		return eb.put(event);
	}

	@GET
	@Path("/event/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Event peekEvent(@PathParam("id") String id) {
		Event event = eb.peek(id);
		if(event != null)
			event.setLastReadTimestamp(System.currentTimeMillis());
		return event;
	}

	@GET
	@Path("/event/group/{group}/name/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Event peekEventByGroupAndName(@PathParam("group") String group, @PathParam("name") String name) {
		Event event = eb.peek(group, name);
		if(event != null)
			event.setLastReadTimestamp(System.currentTimeMillis());
		return event;
	}


	@DELETE
	@Path("/event/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Event consumeEvent(@PathParam("id") String id) {
		Event ret = eb.get(id);
		if(ret != null)
			ret.setDeletionTimestamp(System.currentTimeMillis());
		return ret;
	}


	@DELETE
	@Path("/event/group/{group}/name/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Event consumeEventByGroupAndName(@PathParam("group") String group, @PathParam("name") String name) {
		Event ret = eb.get(group, name);

		if(ret != null)
			ret.setDeletionTimestamp(System.currentTimeMillis());

		return ret;
	}

	@DELETE
	@Path("/events")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> clear() {
		Map<String, Object> ret = new HashMap<>();
		eb.clear();
		ret.put("status","success");
		return ret;
	}

	@DELETE
	@Path("/events/group/{group}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Map<String, Object> clearGroup(@PathParam("group") String group) {
		Map<String, Object> ret = new HashMap<>();
		eb.clearGroup(group);
		ret.put("status","success");
		return ret;
	}

	@GET
	@Path("/events/monitoring/global")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Map<String, Object> getStats() {
		Map<String, Object> stats = new HashMap<>();
		// static conf
		stats.put("s_advStatsOn", eb.getAdvancedStatsOn());
		stats.put("s_circuitBreakerThresholt", eb.getCircuitBreakerThreshold());

		//dynamic stats
		stats.put("d_size", eb.getSize());
		stats.put("d_youngestEvent", eb.findYoungestEvent());
		stats.put("d_oldestEvent", eb.findOldestEvent());

		if(eb.getAdvancedStatsOn()){
			stats.put("a_cumulatedPuts", eb.getCumulatedPuts());
			stats.put("a_cumulatedGets", eb.getCumulatedGets());
			stats.put("a_cumulatedPeeks", eb.getCumulatedPeeks());
		}
		return stats;
	}

	@GET
	@Path("/events/monitoring/group/{group}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Map<String, Object> getGroupStats(@PathParam("group") String group) throws Exception {
		if(group == null || group.isEmpty())
			throw new Exception("Groupname is null or empty.");

		Map<String, Object> stats = new HashMap<>();
		stats.put("g_"+group+"_size", eb.getSizeForGroup(group));
		stats.put("g_"+group+"youngestEvent", eb.findYoungestEventForGroup(group));
		stats.put("g_"+group+"oldestEvent", eb.findOldestEventForGroup(group));
		return stats;
	}

	@GET
	@Path("/events/monitoring/clear")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Map<String, Object> clearStats() {
		Map<String, Object> ret = new HashMap<>();
		eb.clearStats();
		ret.put("status","success");
		return ret;
	}

}
