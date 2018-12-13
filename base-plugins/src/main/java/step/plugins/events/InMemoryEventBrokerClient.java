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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author doriancransac
 *
 */
public class InMemoryEventBrokerClient implements EventBrokerClient{
	
	private EventBroker eb;
	
	public InMemoryEventBrokerClient(EventBroker eb){
		this.eb = eb;
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#getEventBrokerIdMap()
	 */
	@Override
	public Map<String, Event> getEventBrokerIdMap() {
		return eb.getIdBasedEventMap();
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#getEventBrokerIdMap(int, int)
	 */
	@Override
	public Map<String, Event> getEventBrokerIdMap(int skip, int limit) {
		return eb.getIdBasedEventMap(skip, limit);
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#getEventBrokerGroupMap()
	 */
	@Override
	public Map<String, Set<Event>> getEventBrokerGroupMap() {
		return eb.getFullGroupBasedEventMap();
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#getEventBrokerGroupMap(int, int)
	 */
	@Override
	public Map<String, Set<Event>> getEventBrokerGroupMap(int skip, int limit) {
		return eb.getGroupBasedEventMap(skip, limit);
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#putEvent(step.plugins.events.Event)
	 */
	@Override
	public Event putEvent(Event event) {
		try {
			return eb.put(event);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#peekEvent(java.lang.String)
	 */
	@Override
	public Event peekEvent(String id) {
		return eb.peek(id);
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#peekEventByGroupAndName(java.lang.String, java.lang.String)
	 */
	@Override
	public Event peekEventByGroupAndName(String group, String name) {
		return eb.peek(group, name);
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#consumeEvent(java.lang.String)
	 */
	@Override
	public Event consumeEvent(String id) {
		return eb.get(id);
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#consumeEventByGroupAndName(java.lang.String, java.lang.String)
	 */
	@Override
	public Event consumeEventByGroupAndName(String group, String name) {
		return eb.get(group, name);
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#clear()
	 */
	@Override
	public Map<String, Boolean> clear() {
		eb.clear();
		Map<String, Boolean> successMap = new HashMap<>();
		successMap.put("success", Boolean.TRUE);
		return successMap;
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#clearGroup(java.lang.String)
	 */
	@Override
	public Map<String, Boolean> clearGroup(String group) {
		eb.clearStats();
		Map<String, Boolean> successMap = new HashMap<>();
		successMap.put("success", Boolean.TRUE);
		return successMap;
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#getGlobalStats()
	 */
	@Override
	public Map<String, Object> getGlobalStats() {
		return eb.getStats();
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#getGroupStats(java.lang.String)
	 */
	@Override
	public Map<String, Object> getGroupStats(String group) {
		return eb.getGroupStats(group);
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#clearStats()
	 */
	@Override
	public Map<String, Object> clearStats() {
		Map<String, Object> successMap = new HashMap<>();
		successMap.put("success", "true");
		return successMap;
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#getGroupSkipLimit(java.lang.String, int, int)
	 */
	@Override
	public Set<Event> getGroupSkipLimit(String group, int skip, int limit) {
		return eb.getGroupEvents(group, skip, limit);
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#getFullGroup(java.lang.String)
	 */
	@Override
	public Set<Event> getFullGroup(String group) {
		return eb.getGroupEvents(group);
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#getDistinctGroupNames()
	 */
	@Override
	public Set<String> getDistinctGroupNames() {
		return eb.getDistinctGroupNames();
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#getGroupSize(java.lang.String)
	 */
	@Override
	public int getGroupSize(String group) {
		return eb.getSizeForGroup(group);
	}

	/* (non-Javadoc)
	 * @see step.plugins.events.EventBrokerClient#setCircuitBreakerThreshold(long)
	 */
	@Override
	public Map<String, Object> setCircuitBreakerThreshold(long circuitBreakerThreshold) {
		eb.setCircuitBreakerThreshold(circuitBreakerThreshold);
		Map<String, Object> successMap = new HashMap<>();
		successMap.put("success", "true");
		return successMap;
	}

	/* (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		// nothing to do here
	}

}
