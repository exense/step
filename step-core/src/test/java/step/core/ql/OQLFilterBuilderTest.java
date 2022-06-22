/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.core.ql;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import step.core.collections.Filter;
import step.core.collections.PojoFilter;
import step.core.collections.PojoFilters.PojoFilterFactory;

public class OQLFilterBuilderTest {

	public static class Bean {
		String property1 = "prop1";
		String property2 = "prop with some \"\"";
		Bean2 bean1 = new Bean2();
		Map<String, String> map1 = new HashMap<>();
		public Bean() {
			super();
			map1.put("property2", "prop2");
		}
		public String getProperty1() {
			return property1;
		}
		public void setProperty1(String property1) {
			this.property1 = property1;
		}
		public String getProperty2() {
			return property2;
		}
		public void setProperty2(String property2) {
			this.property2 = property2;
		}
		public Bean2 getBean1() {
			return bean1;
		}
		public void setBean1(Bean2 bean1) {
			this.bean1 = bean1;
		}
		public Map<String, String> getMap1() {
			return map1;
		}
		public void setMap1(Map<String, String> map1) {
			this.map1 = map1;
		}
	}
	
	public static class Bean2 {
		String property1 = "prop1";

		public String getProperty1() {
			return property1;
		}

		public void setProperty1(String property1) {
			this.property1 = property1;
		}
	}
	
	@Test
	public void test() {
		PojoFilter<Object> filter = filter("property1=prop1");
		boolean test = filter.test(new Bean());
		assertTrue(test);
	}

	private PojoFilter<Object> filter(String expression) {
		Filter filter = OQLFilterBuilder.getFilter(expression);
		PojoFilter<Object> pojoFilter = new PojoFilterFactory<Object>().buildFilter(filter);
		return pojoFilter;
	}
	
	@Test
	public void test2() {
		PojoFilter<Object> filter = filter("property1=prop1 and bean1.property1=prop1");
		boolean test = filter.test(new Bean());
		assertTrue(test);
	}
	
	@Test
	public void test3() {
		PojoFilter<Object> filter = filter("property1=prop1 and bean1.property1=prop1 and map1.property2=prop2");
		boolean test = filter.test(new Bean());
		assertTrue(test);
	}
	
	@Test
	public void test4() {
		PojoFilter<Object> filter = filter("property1=wrongValue and bean1.property1=prop1 and map1.property2=prop2");
		boolean test = filter.test(new Bean());
		assertFalse(test);
	}

	@Test
	public void test5() {
		PojoFilter<Object> filter = filter("map1.wrongProperty=prop2");
		boolean test = filter.test(new Bean());
		assertFalse(test);
	}
	
	@Test
	public void test6() {
		PojoFilter<Object> filter = filter("");
		boolean test = filter.test(new Bean());
		assertTrue(test);
	}
	
	@Test
	public void test7() {
		PojoFilter<Object> filter = filter(null);
		boolean test = filter.test(new Bean());
		assertTrue(test);
	}
	
	@Test
	public void test8() {
		PojoFilter<Object> filter = filter("not(property1=prop1)");
		boolean test = filter.test(new Bean());
		assertFalse(test);
	}
	
	@Test
	public void test9() {
		PojoFilter<Object> filter = filter("not(property1=prop1) or bean1.property1=prop1");
		boolean test = filter.test(new Bean());
		assertTrue(test);
	}
	
	@Test
	public void test10() {
		PojoFilter<Object> filter = filter("property1=\"prop1\"");
		boolean test = filter.test(new Bean());
		assertTrue(test);
	}
	
	@Test
	public void test11() {
		PojoFilter<Object> filter = filter("property2=\"prop with some \"\"\"\"\"");
		boolean test = filter.test(new Bean());
		assertTrue(test);
	}
}
