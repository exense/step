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

import java.io.Closeable;
import java.util.Map;
import java.util.Set;

public interface EventBrokerClient extends Closeable{
	
	public Map<String, Event> getEventBrokerIdMap();

	public Map<String, Event> getEventBrokerIdMap(int skip, int limit);

	public Map<String, Set<Event>> getEventBrokerGroupMap();

	public Map<String, Set<Event>> getEventBrokerGroupMap(int skip, int limit);

	public Event putEvent(Event event);

	public Event peekEvent(String id);

	public Event peekEventByGroupAndName(String group, String name);

	public Event consumeEvent(String id);

	public Event consumeEventByGroupAndName(String group, String name);

	public Map<String, Boolean> clear();
	
	public Map<String, Boolean> clearGroup(String group);

	public Map<String, Object> getGlobalStats();

	public Map<String, Object> getGroupStats(String group);

	public Map<String, Object> clearStats();

	public Set<Event> getGroupSkipLimit(String group, int skip, int limit);

	public Set<Event> getFullGroup(String group);

	public Set<String> getDistinctGroupNames();

	public int getGroupSize(String group);

	public Map<String, Object> setCircuitBreakerThreshold(long circuitBreakerThreshold);
	
}
