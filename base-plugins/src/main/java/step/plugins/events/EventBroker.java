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

import java.util.Comparator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author doriancransac
 *
 */
public class EventBroker {

	private ConcurrentHashMap<String, Event> events;
	private long circuitBreakerThreshold;
	private boolean advancedStatsOn;

	private LongAdder cumulatedPuts;
	private LongAdder cumulatedGets;
	private LongAdder cumulatedPeeks;

	private int sizeWaterMark = 0;

	public EventBroker(){
		this.circuitBreakerThreshold = 5000L;
		this.advancedStatsOn = true;
		init();
	}

	public EventBroker(long circuitBreakerThreshold, boolean advancedStatsOn){
		this.circuitBreakerThreshold = circuitBreakerThreshold;
		this.advancedStatsOn = advancedStatsOn;
		init();
	}

	private void init(){
		events = new ConcurrentHashMap<String, Event>();
		initStats();
	}

	public Event put(Event event) throws Exception{
		if(event == null)
			throw new Exception("Event is null.");
		else
			event.setInsertionTimestamp(System.currentTimeMillis());

		Event ret = null;
		Event putRetEvent = null;
		String mapKey = null;

		int size = events.size();

		if(size >= this.circuitBreakerThreshold)
			throw new Exception("Broker size exceeds " + this.circuitBreakerThreshold + " events. Circuit breaker is on.");

		if(event.getId() == null || event.getId().isEmpty()){
			if(event.getGroup() == null || event.getGroup().isEmpty()){
				throw new Exception("Event has neither an explicit id or a group. Can't add it.");
			}else{
				mapKey = UUID.randomUUID().toString();
				event.setId(mapKey);

				//we're in the Group use case, so we prefer to return the event itself (benefit: returning the uuid to the user) 
				ret = event;
			}
		}else{
			mapKey = event.getId();
		} 

		// we want to return the previous value in the Id use case (putRetEvent)
		putRetEvent = events.put(mapKey, event);

		if(this.advancedStatsOn){
			this.cumulatedPuts.increment();

			if(size > this.sizeWaterMark){ 
				this.sizeWaterMark = size;
			}
		}

		return ret==null?putRetEvent:ret;
	}

	public void clear(){
		events.clear();
	}

	public Event get(String id){
		if(id == null || id.isEmpty())
			return null;
		Event ret = events.remove(id);
		if(this.advancedStatsOn){
			if(ret != null) //we only count "real" gets (which found and returned an event)
				this.cumulatedGets.increment();
		}
		if(ret != null)
			ret.setDeletionTimestamp(System.currentTimeMillis());
		return ret;
	}

	public Event peek(String id){
		if(id == null || id.isEmpty())
			return null;
		Event ret = events.get(id);
		if(this.advancedStatsOn){
			if(ret != null) //we only count "real" peeks (which found and returned an event)
				this.cumulatedPeeks.increment();
		}
		if(ret != null)
			ret.setLastReadTimestamp(System.currentTimeMillis());
		return ret;
	}

	public Event peek(String group, String name){
		return peek(lookup(group,name));
	}

	public String lookupOld(String searchedGroup, String searchedName){

		if(searchedGroup == null || searchedGroup.isEmpty() || searchedGroup.equals("null"))
			throw new RuntimeException("group can not be null, empty or \"null\", found value=" + searchedGroup);

		String id = null;

		try{
			id = events.values().stream().filter(event -> 
			{
				String eventGroup = event.getGroup();
				String eventName = event.getName();
				// 1) we find an event matching this group
				if((eventGroup != null && eventGroup.equals(searchedGroup)) || searchedGroup.equals("*")){
					// 2) special case: if the searched name is null or empty or has value "null", it's a match regardless of the event's own name
					if(searchedName == null || searchedName.isEmpty() || searchedName.equals("null"))
						return true;
					else{
						// 3) if we're looking for a legit / specific name, then it needs to match exactly that of the event's
						// 3.1) special case: first we discard events which don't have a legit name
						if(eventName == null || eventName.isEmpty() || eventName.equals("null")){
							return false;
						}else{// 3.2) the event has a legit name
							if(searchedName.equals(eventName))// match
								return true;
							else{
								// Wrong name
								return false;
							}
						}
					}
				}else{// Wrong group
					return false;
				}
			}).findAny().get().getId();
		}catch(NoSuchElementException e){}

		return id;
	}

	public String lookup(String searchedGroup, String searchedName){
		if(searchedGroup == null || searchedGroup.isEmpty() || searchedGroup.equals("null"))
			throw new RuntimeException("group can not be null, empty or \"null\", found value=" + searchedGroup);

		if(searchedGroup.equals("*") || searchedName == null || searchedName.isEmpty() || searchedName.equals("null"))
			return lookupLooseGroupBasedEvent(searchedGroup);
		else
			return lookupNamedGroupBasedEvent(searchedGroup, searchedName);
	}
	
	public String lookupNamedGroupBasedEvent(String searchedGroup, String searchedName){
		return events.values().stream()
				.filter(e -> e.getGroup().equals(searchedGroup) || searchedGroup.equals("*"))
				.filter(e -> e.getName().equals(searchedName))
				.findAny().get().getId();
	}

	public String lookupLooseGroupBasedEvent(String searchedGroup){
		return events.values().stream()
				.filter(e -> e.getGroup().equals(searchedGroup) || searchedGroup.equals("*"))
				.findAny().get().getId();
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


	public long getCircuitBreakerThreshold() {
		return circuitBreakerThreshold;
	}

	public void setCircuitBreakerThreshold(long circuitBreakerThreshold) {
		this.circuitBreakerThreshold = circuitBreakerThreshold;
	}

	public int getSize(){
		return events.size();
	}

	public boolean isStatsOn() {
		return advancedStatsOn;
	}

	public void setAdvancedStatsOn(boolean statsOn) {
		this.advancedStatsOn = statsOn;
	}

	public boolean getAdvancedStatsOn() {
		return this.advancedStatsOn;
	}

	public Event findOldestEvent(){
		return events.values().stream().min(Comparator.comparing(Event::getInsertionTimestamp)).get();
	}

	public Event findYoungestEvent(){
		return events.values().stream().max(Comparator.comparing(Event::getInsertionTimestamp)).get();
	}

	public long getCumulatedPuts() {
		return cumulatedPuts.longValue();
	}

	public long getCumulatedGets() {
		return cumulatedGets.longValue();
	}

	public long getCumulatedPeeks() {
		return cumulatedPeeks.longValue();
	}

	public int getSizeWaterMark() {
		return sizeWaterMark;
	}

	public Object getSizeForGroup(String group) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object findYoungestEventForGroup(String group) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object findOldestEventForGroup(String group) {
		// TODO Auto-generated method stub
		return null;
	}

	public void clearStats() {
		initStats();
	}

	private void initStats(){
		this.cumulatedPuts = new LongAdder();
		this.cumulatedGets = new LongAdder();
		this.cumulatedPeeks = new LongAdder();
	}

	public Set<Event> getGroupEvents(String group){
		return getGroupEvents(group, 0, Integer.MAX_VALUE);
	}

	public Set<Event> getGroupEvents(String group, int skip, int limit){
		return events.values().stream()
				.filter(e -> e.getGroup().equals(group))
				.skip(skip)
				.limit(limit)
				.collect(Collectors.toSet());
	}


	public Map<String, Set<Event>> getFullGroupBasedEventMap(){
		return getFullGroupBasedEventMap(0, Integer.MAX_VALUE);
	}

	public Map<String, Set<Event>> getFullGroupBasedEventMap(int skip, int limit){
		return events.values().stream()
				.filter(distinctByKey(Event::getGroup))
				.skip(skip)
				.limit(limit)
				.collect(Collectors.groupingBy(Event::getGroup, Collectors.toSet()));
	}

	public Set<String> getDistinctGroupList(){
		//return getFullGroupBasedEventMap().keySet();
		return events.values().stream()
				.map(e -> e.getGroup())
				.distinct()
				.collect(Collectors.toSet());
	}

	public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
		Set<Object> seen = ConcurrentHashMap.newKeySet();
		return t -> seen.add(keyExtractor.apply(t));
	}
}

