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
import java.util.Set;

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
	@Path("/events/asIdMap")
	@Produces(MediaType.APPLICATION_JSON)
	//@Consumes(MediaType.APPLICATION_JSON)
	public Map<String, Event> getEventBrokerIdMap() {
		return eb.getIdBasedEventMap();
	}
	
	@GET
	@Path("/events/asGroupMap")
	@Produces(MediaType.APPLICATION_JSON)
	//@Consumes(MediaType.APPLICATION_JSON)
	public Map<String, Set<Event>> getEventBrokerGroupMap() {
		return eb.getFullGroupBasedEventMap();
	}

	@GET
	@Path("/events/asIdMap/skip/{skip}/limit/{limit}")
	@Produces(MediaType.APPLICATION_JSON)
	//@Consumes(MediaType.APPLICATION_JSON)
	public Map<String, Event> getEventBrokerIdMap(@PathParam("skip") int skip, @PathParam("limit") int limit) {
		return eb.getIdBasedEventMap(skip, limit);
	}
	
	@GET
	@Path("/events/asGroupMap/skip/{skip}/limit/{limit}")
	@Produces(MediaType.APPLICATION_JSON)
	//@Consumes(MediaType.APPLICATION_JSON)
	public Map<String, Set<Event>> getEventBrokerGroupMap(@PathParam("skip") int skip, @PathParam("limit") int limit) {
		return eb.getGroupBasedEventMap(skip, limit);
	}

	@POST
	@Path("/event")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Event putEvent(Event event) throws Exception {
		return eb.put(event);
	}

	@GET
	@Path("/event/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Event peekEvent(@PathParam("id") String id) {
		return eb.peek(id);
	}

	@GET
	@Path("/event/group/{group}/name/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Event peekEventByGroupAndName(@PathParam("group") String group, @PathParam("name") String name) {
		return eb.peek(group, name);
	}

	@GET
	@Path("/events/group/{group}/skip/{skip}/limit/{limit}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Set<Event> getGroupSkipLimit(@PathParam("group") String group, @PathParam("skip") int skip, @PathParam("limit") int limit) {
		return eb.getGroupEvents(group, skip, limit);
	}
	
	@GET
	@Path("/events/group/{group}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Set<Event> getFullGroup(@PathParam("group") String group) {
		return eb.getGroupEvents(group);
	}
	
	@GET
	@Path("/events/groups")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Set<String> getGroups() {
		return eb.getDistinctGroupNames();
	}

	@GET
	@Path("/events/group/{group}/size")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public int getGroupSize(@PathParam("group") String group) {
		return eb.getSizeForGroup(group);
	}


	@DELETE
	@Path("/event/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Event consumeEvent(@PathParam("id") String id) {
		return eb.get(id);
	}


	@DELETE
	@Path("/event/group/{group}/name/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Event consumeEventByGroupAndName(@PathParam("group") String group, @PathParam("name") String name) {
		return eb.get(group, name);
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
		stats.put("s_advStatsOn", eb.getAdvancedStatsOn());
		
		stats.put("s_circuitBreakerThreshold", eb.getCircuitBreakerThreshold());

		//dynamic stats
		stats.put("d_size", eb.getSize());
		stats.put("d_youngestEvent", eb.findYoungestEvent());
		stats.put("d_oldestEvent", eb.findOldestEvent());

		if(eb.getAdvancedStatsOn()){
			stats.put("a_cumulatedPuts", eb.getCumulatedPuts());
			stats.put("a_cumulatedGets", eb.getCumulatedGets());
			stats.put("a_cumulatedAttemptedGets", eb.getCumulatedAttemptedGets());
			stats.put("a_cumulatedAttemptedGroupGets", eb.getCumulatedAttemptedGroupGets());
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
		stats.put("g_"+group+"_youngestEvent", eb.findYoungestEventForGroup(group));
		stats.put("g_"+group+"_oldestEvent", eb.findOldestEventForGroup(group));
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

	
	/** Conf Services **/
	//TODO extend this to a generic config map with generic GET/POST on properties	
	@GET
	@Path("/events/config/circuitBreakerThreshold/{circuitBreakerThreshold}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Map<String, Object> setCircuitBreakerThreshold(@PathParam("circuitBreakerThreshold") long circuitBreakerThreshold) {
		Map<String, Object> ret = new HashMap<>();
		eb.setCircuitBreakerThreshold(circuitBreakerThreshold);
		ret.put("status","success");
		return ret;
	}
	
	/** **/
}
