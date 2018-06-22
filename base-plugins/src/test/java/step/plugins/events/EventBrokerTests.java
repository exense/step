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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import step.plugins.events.Event;
import step.plugins.events.EventBroker;

/**
 * @author doriancransac
 *
 */
public class EventBrokerTests {

	private EventBroker eb;
	
	@Before
	public void before(){
		eb = new EventBroker();
	}
	
	@After
	public void after(){
		eb = new EventBroker();
	}
	
	@Test
	public void testGetEvent(){
		eb.put(new Event().setId("123").setName("toto").setGroup("myGroup"));
		Assert.assertEquals("toto",eb.get("123").getName());
		Assert.assertEquals("123",eb.get("myGroup", "toto").getId());
	}
	
	@Test
	public void testRemoveEvent(){
		eb.put(new Event().setId("123").setName("toto").setGroup("myGroup"));
		eb.remove("123");
		Assert.assertEquals(false,eb.hasEvent("123"));
		Assert.assertEquals(false,eb.hasEvent("myGroup", "toto"));
	}
}
