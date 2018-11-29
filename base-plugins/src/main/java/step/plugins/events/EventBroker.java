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
package step.plugins.events;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author doriancransac
 *
 */
public class EventBroker {

	private ConcurrentHashMap<String, Event> events;

	public EventBroker(){
		events = new ConcurrentHashMap<String, Event>();
	}

	public Event put(Event event) throws Exception{
		if(event == null)
			throw new Exception("Event is null.");
		
		if(event.getId() == null || event.getId().isEmpty()){
			if(event.getGroup() == null || event.getGroup().isEmpty()){
				throw new Exception("Event has neither an explicit id or a group. Can't add it.");
			}else{
				String mapKey = UUID.randomUUID().toString();
				event.setId(mapKey);
				// In this case (Group based), we're returning the event valued with the internal id instead of always returning a "null mapping"...
				events.put(mapKey, event);
				return event;
			}
		}else{
			return events.put(event.getId(), event);
		}
	}
	
	public void clear(){
		events.clear();
	}

	public Event get(String id){
		if(id == null || id.isEmpty())
			return null;
		Event ret = events.remove(id);
		if(ret != null)
			ret.setDeletionTimestamp(System.currentTimeMillis());
		return ret;
	}
	
	public Event peek(String id){
		if(id == null || id.isEmpty())
			return null;
		Event ret = events.get(id);
		if(ret != null)
			ret.setLastReadTimestamp(System.currentTimeMillis());
		return ret;
	}
	
	public Event peek(String group, String name){
		if(group == null || group.isEmpty())
			return null;
		String id = lookup(group, name);
		if(id == null || id.isEmpty())
			return null;
		Event ret = events.get(id);
		if(ret != null)
			ret.setLastReadTimestamp(System.currentTimeMillis());
		return ret;
	}

	public String lookup(String group, String name){
		
		if(group == null)
			throw new RuntimeException("group can not be null.");
		
		String id = null;
		
		try{
			id = events.values().stream().filter(v -> 
			{
				//TODO: this check can be externalized from the filter for better performance
				// create two disctinct predicates for the filter?
				if(v.getGroup() != null && v.getGroup().equals(group)){
					if(name == null || name.isEmpty())
						return true;
					else{
						if(v.getName().equals(name))
							return true;
					}
				}
				return false;
			}).findAny().get().getId();
		}catch(NoSuchElementException e){}
		
		return id;
	}
	public String toString(){
		return events.toString();
	}

	public void clearGroup(String group) {
		while(hasEvent(group, null))
			remove(lookup(group, null));
	}

	public Map<String, Event> asMap() {
		return events;
	}
	
	
	/* Privatizing to force atomic use of "has+get+remove" in one concept (get) by client */
	private boolean hasEvent(String id){
		if(id == null)
			return false;
		else
			return events.containsKey(id);
	}

	private boolean hasEvent(String group, String name){
		return hasEvent(lookup(group, name));
	}

	public Event get(String group, String name){
		return get(lookup(group, name));
	}
	
	//TODO: see Event class (deletionTimestamp)
	private void remove(String id){
		events.remove(id);
	}

	private void remove(String group, String name){
		remove(lookup(group, name));
	}
	
	/* */
}

