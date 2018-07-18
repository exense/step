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

import java.util.Map;
import java.util.UUID;

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
	public Event putEvent(Event event) {
		if(event != null)
			event.setSubmitionTimestamp(System.currentTimeMillis());
		Event ret = null;
		synchronized(eb){
			ret = eb.put(event);
		}
		return ret;
	}

	@GET
	@Path("/event/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Event peekEvent(@PathParam("id") String id) {
		Event event = eb.get(id);
		if(event != null)
			event.setLastReadTimestamp(System.currentTimeMillis());
		return event;
	}

	@GET
	@Path("/event/group/{group}/name/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Event peekEventByGroupAndName(@PathParam("group") String group, @PathParam("name") String name) {
		Event event = eb.get(group, name);
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
		synchronized(eb){
			eb.remove(id);
		}
		return ret;
	}


	@DELETE
	@Path("/event/group/{group}/name/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Event consumeEventByGroupAndName(@PathParam("group") String group, @PathParam("name") String name) {
		Event ret = null;
		synchronized(eb){
			ret = eb.get(group, name);
			if(ret != null)
				eb.remove(ret.getId());
		}
		if(ret != null)
			ret.setDeletionTimestamp(System.currentTimeMillis());

		return ret;
	}

	@DELETE
	@Path("/events")
	@Produces(MediaType.APPLICATION_JSON)
	public String clear() {
		synchronized(eb){
			eb.clear();
		}
		return "{ \"status\" : \"success\"}";
	}

	@DELETE
	@Path("/events/group/{group}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String clearGroup(@PathParam("group") String group) {
		synchronized(eb){
			eb.clearGroup(group);
		}
		return "{ \"status\" : \"success\"}";
	}

	//For test purposes
	@GET
	@Path("/magicevent")
	@Produces(MediaType.APPLICATION_JSON)
	public Event magicPutEvent() {
		synchronized(eb){
			return putEvent(new Event()
					.setId(UUID.randomUUID().toString())
					.setName("hello")
					.setGroup("testGroup"));
		}
	}
}
