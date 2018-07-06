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

	public Event put(Event event){
		if(event == null || event.getId() == null || event.getId().isEmpty()){
			String uuid = UUID.randomUUID().toString();
			events.put(uuid, event.setId(uuid));
		}
		events.put(event.getId(), event);
		return event;
	}
	
	public void clear(){
		events.clear();
	}

	public Event get(String id){
		if(!hasEvent(id))
			return null;
		else
			return events.get(id).setLastReadTimestamp(System.currentTimeMillis());
	}

	public boolean hasEvent(String id){
		if(id == null)
			return false;
		else
			return events.containsKey(id);
	}

	public boolean hasEvent(String group, String name){
		return hasEvent(lookup(group, name));
	}

	public Event get(String group, String name){
		return get(lookup(group, name));
	}

	//TODO: see Event class (deletionTimestamp)
	public void remove(String id){
		events.remove(id);
	}

	public void remove(String group, String name){
		remove(lookup(group, name));
	}

	//TODO: add and maintain a group-name multimap index for faster lookups
	//TODO: regex support? contains instead of equals?
	public String lookup(String group, String name){
		String id = null;
		
		try{
			id = events.values().stream().filter(v -> 
			{
				if(v.getGroup().equals(group) && v.getName().equals(name))
					return true;
				return false;
			}).findAny().get().getId();
		}catch(NoSuchElementException e){}
		
		return id;
	}
	public String toString(){
		return events.toString();
	}
}
