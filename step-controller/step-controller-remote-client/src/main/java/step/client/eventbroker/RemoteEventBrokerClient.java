/*******************************************************************************
 * (C) Copyright 2016 Dorian Cransac and Jerome Comte
 *  
 * This file is part of rtm
 *  
 * rtm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * rtm is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with rtm.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.client.eventbroker;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.plugins.events.Event;
import step.plugins.events.EventBrokerClient;

//TODO: Move away from restful paths and post parameters instead (null vs "" vs "null" problem)
@SuppressWarnings("unchecked")
public class RemoteEventBrokerClient extends AbstractRemoteClient implements EventBrokerClient{

	public static String servicePath = "/rest/eventbroker";

	//Jackson approach
	//private ObjectMapper mapper = new ObjectMapper();
	//private MapType jacksonMapStringSetEvent = mapper.getTypeFactory().constructMapType(Map.class, String.class, Set.class);

	//Standard Java (current)
	private GenericType<Map<String,Set<Event>>> mapStringSetEvent = new GenericType<Map<String,Set<Event>>>(){};
	private GenericType<Set<Event>> setEvent = new GenericType<Set<Event>>(){};
	private GenericType<Map<String,Event>> mapStringEvent = new GenericType<Map<String,Event>>(){};

	public RemoteEventBrokerClient(ControllerCredentials credentials){
		super(credentials);
	}

	public RemoteEventBrokerClient(){
		super();
	}

	@Deprecated
	public String getEventBrokerStatus() {
		return getEventBrokerGroupMap().toString();
	}

	public Map<String, Event> getEventBrokerIdMap() {
		return executeRequest(()->requestBuilder(servicePath + "/events/asIdMap").get(mapStringEvent));
	}

	public Map<String, Event> getEventBrokerIdMap(int skip, int limit) {
		return executeRequest(()->requestBuilder(servicePath + "/events/asIdMap/skip/"+skip+"/limit/"+limit).get(mapStringEvent));
	}

	public Map<String, Set<Event>> getEventBrokerGroupMap() {
		// Standard Java
		return executeRequest(()->requestBuilder(servicePath + "/events/asGroupMap").get(mapStringSetEvent));
		// Jackson approach
		/*return mapper.convertValue(
				executeRequest(()->requestBuilder(servicePath + "/events/asGroupMap").get(Object.class))
				,mapType);*/
	}

	public Map<String, Set<Event>> getEventBrokerGroupMap(int skip, int limit) {
		return executeRequest(()->requestBuilder(servicePath + "/events/asGroupMap/skip/"+skip+"/limit/"+limit).get(mapStringSetEvent));
	}

	public Event putEvent(Event event) {
		Entity<Event> entity = Entity.entity(event, MediaType.APPLICATION_JSON);
		event.setSubmitionTimestamp(System.currentTimeMillis());
		return  executeRequest(()->requestBuilder(servicePath + "/event").post(entity, Event.class));
	}

	public Event peekEvent(String id) {
		Event event =  executeRequest(()->requestBuilder(servicePath + "/event/"+id).get(Event.class));
		if(event !=null)
			event.setReceptionTimestamp(System.currentTimeMillis());
		return event;
	}

	public Event peekEventByGroupAndName(String group, String name) throws Exception {
		failOnEmptyString("group", group);
		Event event =  executeRequest(()->requestBuilder(servicePath + "/event/group/" + group + "/name/" + name).get(Event.class));
		if(event !=null)
			event.setReceptionTimestamp(System.currentTimeMillis());
		return event;
	}

	public Event consumeEvent(String id) {
		Event event =  executeRequest(()->requestBuilder(servicePath + "/event/"+id).delete(Event.class));
		if(event != null)
			event.setReceptionTimestamp(System.currentTimeMillis());
		return event;
	}

	public Event consumeEventByGroupAndName(String group, String name) throws Exception {
		failOnEmptyString("group", group);
		Event event =  executeRequest(()->requestBuilder(servicePath + "/event/group/" + group + "/name/" + name).delete(Event.class));
		if(event != null)
			event.setReceptionTimestamp(System.currentTimeMillis());
		return event;
	}

	public Map<String, Boolean> clear() {
		return  executeRequest(()->requestBuilder(servicePath + "/events").delete(Map.class));
	}

	public Map<String, Boolean> clearGroup(String group) {
		return  executeRequest(()->requestBuilder(servicePath + "/events/group/"+group).delete(Map.class));
	}


	public Map<String, Object> getGlobalStats() {
		return executeRequest(()->requestBuilder(servicePath + "/events/monitoring/global").get(Map.class));
	}

	public Map<String, Object> getGroupStats(String group) {
		return executeRequest(()->requestBuilder(servicePath + "/events/monitoring/group/" + group).get(Map.class));
	}

	public Map<String, Object> clearStats() {
		return executeRequest(()->requestBuilder(servicePath + "/events/monitoring/clear").get(Map.class));
	}	

	public Set<Event> getGroupSkipLimit(String group, int skip, int limit) throws Exception {
		failOnEmptyString("group", group);
		return executeRequest(()->requestBuilder(servicePath + "/events/group/"+group+"/skip/"+skip+"/limit/"+limit).get(Set.class));
	}

	public Set<Event> getFullGroup(String group) {
		return executeRequest(()->requestBuilder(servicePath + "/events/group/"+group).get(setEvent));
	}

	public Set<String> getDistinctGroupNames() {
		return executeRequest(()->requestBuilder(servicePath + "/events/groups").get(Set.class));
	}

	public int getGroupSize(String group) {
		return executeRequest(()->requestBuilder(servicePath + "/events/group/"+group+"/size").get(Integer.class));
	}

	public Map<String, Object> setCircuitBreakerThreshold(long circuitBreakerThreshold) {
		return executeRequest(()->requestBuilder(servicePath + "/events/config/circuitBreakerThreshold/"+circuitBreakerThreshold).get(Map.class));
	}

	private static void failOnEmptyString(final String stringName, final String value) throws Exception{
		if(value != null){
			if(value.isEmpty()){
				throw new Exception("Empty string not accepted for parameter \""+stringName+"\". Either choose a different method or use the wildcard character \"*\".");
			}
		}
	}
}
