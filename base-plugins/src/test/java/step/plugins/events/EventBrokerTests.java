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
		eb = new EventBroker(1000);
	}

	@After
	public void after(){
		//System.out.println("---\n"+ eb);
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

	@Test
	public void testNoIdNoGroup() throws Exception{
		boolean exceptionRaised = false;
		try{
			Event e = eb.put(new Event().setName("foo"));
		}catch(Exception e){
			exceptionRaised = true;
			Assert.assertEquals(true, e.getMessage().contains("Event has neither an explicit id or a group."));	
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
	public void testCircuitBreaker() throws Exception{
		long circuitBreakerThreshold = 3L;
		eb.setCircuitBreakerThreshold(circuitBreakerThreshold);
		
		for(int i=1; i <= circuitBreakerThreshold; i++){
			Event e = new Event().setGroup("circuitBreaker");
			System.out.println(eb.put(e).getId());
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
}
