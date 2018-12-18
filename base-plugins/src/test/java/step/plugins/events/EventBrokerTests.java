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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author doriancransac
 *
 */
public class EventBrokerTests {

	private EventBroker eb;

	@Before
	public void before(){
		eb = new EventBroker(1000, true);
	}

	@After
	public void after(){
		//System.out.println("---\n"+ eb);
	}

	@Test
	public void testGetAbsentId() throws Exception{
		Assert.assertEquals(null,eb.get("an_id_that_is_absent"));
	}


	@Test
	public void testGetEvent() throws Exception{
		eb.put(new Event().setId("123").setGroup("myGroup").setName("toto"));
		Assert.assertEquals("toto",eb.get("123").getName());
		eb.put(new Event().setId("123").setGroup("myGroup").setName("toto"));
		Assert.assertEquals("123",eb.get("myGroup", "toto").getId());
	}

	@Test
	public void testPeekEvent() throws Exception{
		eb.put(new Event().setId("234").setName("tata").setGroup("thisGroup"));
		Assert.assertEquals("thisGroup",eb.peek("234").getGroup());
		Assert.assertEquals("234",eb.peek("thisGroup", "tata").getId());
	}

	@Test
	public void testPreviousIdValueFresh() throws Exception{
		Event e = eb.put(new Event().setId("uvw"));
		Assert.assertEquals(null, e);
	}

	//Now allowing null group (-> "<default>")
	//@Test
	public void testNoIdNoGroup() throws Exception{

		String invalidEventMessage = "Event has neither an explicit id or a group.";

		assertPutInvalidEventExceptionMessage(new Event().setName("foo"), invalidEventMessage);
		assertPutInvalidEventExceptionMessage(new Event().setId(""), invalidEventMessage);
		assertPutInvalidEventExceptionMessage(new Event().setId(null), invalidEventMessage);
		assertPutInvalidEventExceptionMessage(new Event().setGroup(""), invalidEventMessage);
		assertPutInvalidEventExceptionMessage(new Event().setGroup(null), invalidEventMessage);
	}

	public void assertPutInvalidEventExceptionMessage(Event e, String message){
		boolean exceptionRaised = false;
		try{
			eb.put(e);
		}catch(Exception e1){
			exceptionRaised = true;
			Assert.assertEquals(true, e1.getMessage().contains(message));			
		}
		Assert.assertEquals(true, exceptionRaised);
	}

	@Test
	public void testPreviousValueOverride() throws Exception{

		String valueKey = "value";

		Map<String, Object> first = new HashMap<>();
		Map<String, Object> second = new HashMap<>();
		Map<String, Object> third = new HashMap<>();

		first.put(valueKey, "1");
		second.put(valueKey, "2");
		third.put(valueKey, "3");

		Event e1 = eb.put(new Event().setId("ijk").setPayload(first));
		//e1 should be null since the previous event associated with id "ijk" was null (no mapping) 
		Event e2 = eb.put(new Event().setId("ijk").setPayload(second));
		//e1 is now overridden inside the broker, e2 has the value of the first even we sent (which is not e1 though!)
		Assert.assertNotNull(null, e2);
		Assert.assertEquals("1", e2.getPayload().get(valueKey));
		Event e3 = eb.put(new Event().setId("ijk").setPayload(third));
		Assert.assertEquals("2", e3.getPayload().get(valueKey));
		Event e4 = eb.get("ijk");
		Assert.assertEquals("3", e4.getPayload().get(valueKey));
	}

	@Test
	public void testNoGroupOverride() throws Exception{
		String valueKey = "value";

		Map<String, Object> first = new HashMap<>();
		Map<String, Object> second = new HashMap<>();

		first.put(valueKey, "1");
		second.put(valueKey, "2");

		Event e1 = eb.put(new Event().setGroup("aGroup").setPayload(first));
		Event e2 = eb.put(new Event().setGroup("aGroup").setPayload(second));

		Assert.assertNotNull(null, e2);
		// Consume 1st event
		Event e3 = eb.get("aGroup", null);
		// Consume 2st event
		Event e4 = eb.get("aGroup", null);

		// We've got two different events (or at least different eventIds)
		Assert.assertEquals(true, !e3.getId().equals(e4.getId()));

		// 1st event has either value 1 or 2 (no ordering garantee)
		Assert.assertEquals(true, e3.getPayload().get(valueKey).equals("1") || e3.getPayload().get(valueKey).equals("2"));

		// 1st event has either value 1 or 2 (no ordering garantee)
		Assert.assertEquals(true, e4.getPayload().get(valueKey).equals("1") || e4.getPayload().get(valueKey).equals("2"));
	}

	@Test
	public void testSkipLimit() throws Exception{
		eb.put(new Event().setGroup("A"));
		eb.put(new Event().setGroup("B"));
		eb.put(new Event().setGroup("C"));
		eb.put(new Event().setGroup("D"));
		
		Assert.assertEquals(1, eb.getIdBasedEventMap(1, 2).size());
		Assert.assertEquals(1, eb.getIdBasedEventMap(2, 3).size());
		Assert.assertEquals(2, eb.getIdBasedEventMap(2, 4).size());
		
		Assert.assertEquals(1, eb.getGroupBasedEventMap(1, 2).size());
		Assert.assertEquals(1, eb.getGroupBasedEventMap(2, 3).size());
		Assert.assertEquals(2, eb.getGroupBasedEventMap(2, 4).size());
	}
	
	@Test
	public void testCircuitBreaker() throws Exception{
		long circuitBreakerThreshold = 3L;
		eb.setCircuitBreakerThreshold(circuitBreakerThreshold);

		for(int i=1; i <= circuitBreakerThreshold; i++){
			eb.put(new Event().setGroup("circuitBreaker"));
		}

		boolean exceptionRaised = false;
		try{
			eb.put(new Event().setGroup("circuitBreaker"));
		}catch(Exception e1){
			exceptionRaised = true;
			Assert.assertEquals(true, e1.getMessage().contains("Broker size exceeds"));
		}
		Assert.assertEquals(true, exceptionRaised);
	}

	@Test
	public void testMonitoringEventAge() throws Exception{
		for(int i=1; i <= 3; i++){
			eb.put(new Event().setId(String.valueOf(i)));
			Thread.sleep(10);
		}

		Assert.assertEquals("1", eb.findOldestEvent().getId());
		Assert.assertEquals("3", eb.findYoungestEvent().getId());
	}

	@Test
	public void testMonitoringCumulatedStats() throws Exception{
		for(int i=1; i <= 3; i++){
			eb.put(new Event().setId(String.valueOf(i)));
			Thread.sleep(10);
		}

		Assert.assertEquals(3, eb.getCumulatedPuts());

		for(int i=1; i <= 3; i++){
			eb.get(String.valueOf(i));
			Thread.sleep(10);
		}

		Assert.assertEquals(3, eb.getCumulatedGets());

		//Failed get
		eb.get("bar");

		Assert.assertEquals(3, eb.getCumulatedGets());

		Assert.assertEquals(0, eb.getCumulatedPeeks());

		//Failed peek
		eb.peek("bar");
		Assert.assertEquals(0, eb.getCumulatedPeeks());

		eb.put(new Event().setId("bar"));
		eb.peek("bar");
		Assert.assertEquals(1, eb.getCumulatedPeeks());
	}

	@Test
	public void testDistinctGroupList() throws Exception{

		eb.put(new Event().setGroup("foo"));
		eb.put(new Event().setGroup("foo"));
		eb.put(new Event().setGroup("bar"));

		Assert.assertEquals(2, eb.getDistinctGroupNames().size());
		Assert.assertEquals(true, eb.getDistinctGroupNames().contains("foo"));
		Assert.assertEquals(true, eb.getDistinctGroupNames().contains("bar"));
	}
	
	@Test
	public void testWildCards() throws Exception{
		String id = null;
		id = eb.put(new Event().setGroup("lorel")).getId();
		Assert.assertEquals(id, eb.get("*", "*").getId());
		
		id = eb.put(new Event().setGroup("lorel").setName("hardy")).getId();
		Assert.assertEquals(id, eb.get("*", "hardy").getId());
		
		id = eb.put(new Event().setGroup("lorel").setName("hardy")).getId();
		Assert.assertEquals(id, eb.get("lorel", "*").getId());
		
		id = eb.put(new Event().setName("hardy")).getId();
		Assert.assertEquals(id, eb.get("*", "hardy").getId());
		
		id = eb.put(new Event().setName("hardy")).getId();
		Assert.assertEquals(id, eb.get(null, "hardy").getId());
		
		id = eb.put(new Event().setGroup("lorel")).getId();
		Assert.assertNull(eb.get(null, "hardy"));

		eb.clear();
		
		id = eb.put(new Event().setGroup("lorel").setName("hardy")).getId();
		Assert.assertEquals(id, eb.get(null, null).getId());
	}

	@Test
	public void testGroupSizeAndContent() throws Exception{
		eb.put(new Event().setGroup("foo"));
		eb.put(new Event().setGroup("foo"));
		eb.put(new Event().setGroup("bar"));

		Assert.assertEquals(2, eb.getGroupEvents("foo").size());
		Assert.assertEquals(2, eb.getSizeForGroup("foo"));
		Assert.assertEquals(1, eb.getGroupEvents("bar").size());
		Assert.assertEquals(1, eb.getSizeForGroup("bar"));

		eb.get("*", null);
		eb.get("*", null);
		eb.get("*", null);

		Assert.assertEquals(0, eb.getGroupEvents("foo").size());
		Assert.assertEquals(0, eb.getSizeForGroup("bar"));

	}


	@Test
	public void testGroupStats() throws Exception{
		eb.put(new Event().setGroup("xyz").setName("1"));
		Thread.sleep(10);
		eb.put(new Event().setGroup("xyz").setName("2"));

		Assert.assertEquals("1", eb.findOldestEventForGroup("xyz").getName());
		Assert.assertEquals("2", eb.findYoungestEventForGroup("xyz").getName());
		Assert.assertEquals(true, eb.findOldestEventForGroup("xyz").getInsertionTimestamp() < eb.findYoungestEventForGroup("xyz").getInsertionTimestamp());
	}

	@Test
	public void testParallelGroupPutGetByPair() throws Exception{
		int nbThreads = 500;
		final int nbIterations = 100;
		final String group = UUID.randomUUID().toString();

		ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
		for(int threadNb = 0; threadNb<nbThreads; threadNb++){
			final String threadId = String.format("%07d", threadNb);
			executor.execute(new Runnable(){

				@Override
				public void run() {
					for(int i=0;i<nbIterations; i++){
						final String itId = String.format("%07d", i);
						final String identifier = "T"+threadId+"_I"+itId;
						//String uuid = UUID.randomUUID().toString();
						String uuid = identifier;
						try {
							Event e = eb.put(new Event().setGroup(group).setName(uuid));
							Assert.assertEquals(true, e != null && e.getId() != null && !e.getId().isEmpty());
						} catch (Exception e) {e.printStackTrace();}
						Event e = eb.get(group, uuid);
						Assert.assertEquals(group, e.getGroup());
						Assert.assertEquals(uuid, e.getName());
					}
				}
			});
		}
		executor.shutdown();
		Assert.assertEquals(true,executor.awaitTermination(2L, TimeUnit.MINUTES));

		System.out.println("[testParallelGroupPutGetByPair] Size=" + eb.getSize() +"; Watermark=" + eb.getSizeWaterMark() + "; Puts="+eb.getCumulatedPuts() + "; Gets=" + eb.getCumulatedGets() + ";");
		
		// Make sure we put enough pressure on the CHM -- not reliable
		//Assert.assertEquals(true, eb.getSizeWaterMark() > 1 && eb.getSizeWaterMark() < eb.getCircuitBreakerThreshold());

		//Actual checks
		Assert.assertEquals(0, eb.getSize());
		Assert.assertEquals(nbThreads*nbIterations, eb.getCumulatedPuts());
		Assert.assertEquals(nbThreads*nbIterations, eb.getCumulatedGets());
	}
	
	@Test
	public void testParallelIdPutGetByPair() throws Exception{
		int nbThreads = 500;
		final int nbIterations = 1000;
		final String group = UUID.randomUUID().toString();

		ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
		for(int threadNb = 0; threadNb<nbThreads; threadNb++){
			final String threadId = String.format("%07d", threadNb);
			executor.execute(new Runnable(){

				@Override
				public void run() {
					for(int i=0;i<nbIterations; i++){
						final String itId = String.format("%07d", i);
						final String identifier = "T"+threadId+"_I"+itId;
						//String uuid = UUID.randomUUID().toString();
						String uuid = identifier;
						try {
							Event e = eb.put(new Event().setId(uuid));
							Assert.assertEquals(true, e == null);
						} catch (Exception e) {e.printStackTrace();}
						Event e = eb.get(uuid);
						Assert.assertEquals(uuid, e.getId());
					}
				}
			});
		}
		executor.shutdown();
		Assert.assertEquals(true,executor.awaitTermination(2L, TimeUnit.MINUTES));

		System.out.println("[testParallelIdPutGetByPair] Size=" + eb.getSize() +"; Watermark=" + eb.getSizeWaterMark() + "; Puts="+eb.getCumulatedPuts() + "; Gets=" + eb.getCumulatedGets() + ";");
		
		// Make sure we put enough pressure on the broker
		Assert.assertEquals(true, eb.getSizeWaterMark() > 1 && eb.getSizeWaterMark() < eb.getCircuitBreakerThreshold());

		//Actual checks
		Assert.assertEquals(0, eb.getSize());
		Assert.assertEquals(nbThreads*nbIterations, eb.getCumulatedPuts());
		Assert.assertEquals(nbThreads*nbIterations, eb.getCumulatedGets());
	}
	
	@Test
	public void testParallelGroupPutBeforeGet() throws Exception{
		int nbThreads = 100;
		final int nbIterations = 100;
		final int totalExpectedEvents = nbThreads*nbIterations;
		eb.setCircuitBreakerThreshold(totalExpectedEvents + 1);
		final String group = UUID.randomUUID().toString();

		ExecutorService executorPuts = Executors.newFixedThreadPool(nbThreads);
		for(int threadNb = 0; threadNb<nbThreads; threadNb++){
			final String threadId = String.format("%07d", threadNb);
			executorPuts.execute(new Runnable(){

				@Override
				public void run() {
					for(int i=0;i<nbIterations; i++){
						final String itId = String.format("%07d", i);
						final String identifier = "T"+threadId+"_I"+itId;
						try {
							Event e = eb.put(new Event().setGroup(group).setName(identifier));
							Assert.assertEquals(true, e != null && e.getId() != null && !e.getId().isEmpty());
						} catch (Exception e) {e.printStackTrace();}
					}
				}
			});
		}
		executorPuts.shutdown();
		Assert.assertEquals(true,executorPuts.awaitTermination(1L, TimeUnit.MINUTES));
		
		System.out.println("Putters finished :"+eb.getSize() + " events in map; Watermark=" + eb.getSizeWaterMark() + "; Puts="+eb.getCumulatedPuts() + "; Gets=" + eb.getCumulatedGets() + "; Starting getters");
		// Unreliable due to CHM
		//Assert.assertEquals(true, eb.getSizeWaterMark() == totalExpectedEvents);
		Assert.assertEquals(true, eb.getSize() == totalExpectedEvents);

		ExecutorService executorGets = Executors.newFixedThreadPool(nbThreads);
		for(int threadNb = 0; threadNb<nbThreads; threadNb++){
			final String threadId = String.format("%07d", threadNb);
			executorGets.execute(new Runnable(){

				@Override
				public void run() {
					for(int i=0;i<nbIterations; i++){
						final String itId = String.format("%07d", i);
						final String identifier = "T"+threadId+"_I"+itId;
						Event e = eb.get(group, identifier);
						Assert.assertEquals(group, e.getGroup());
						Assert.assertEquals(identifier, e.getName());
					}
				}
			});
		}
		executorGets.shutdown();
		Assert.assertEquals(true, executorGets.awaitTermination(10L, TimeUnit.MINUTES));
		
		System.out.println("[testParallelGroupPutBeforeGet] Size=" + eb.getSize() +"; Watermark=" + eb.getSizeWaterMark() + "; Puts="+eb.getCumulatedPuts() + "; Gets=" + eb.getCumulatedGets() + "; AttemptedGets=" + eb.getCumulatedAttemptedGets() + "; AttemptedGroupGets=" + eb.getCumulatedAttemptedGroupGets() + ";");
		
		// Making sure we were unimpeded by the circuitBreaker
		Assert.assertEquals(true, eb.getSizeWaterMark() < eb.getCircuitBreakerThreshold());
		 
		// Unreliable due to CHM
		//Assert.assertEquals(true, eb.getSizeWaterMark() == totalExpectedEvents);
		Assert.assertEquals(0, eb.getSize());
		Assert.assertEquals(totalExpectedEvents, eb.getCumulatedPuts());
		Assert.assertEquals(totalExpectedEvents, eb.getCumulatedGets());
}

}
