package step.core.ql;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import junit.framework.Assert;
import step.core.ql.Filter;
import step.core.ql.OQLFilterBuilder;

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
		Filter<Object> filter = OQLFilterBuilder.getFilter("property1=prop1");
		boolean test = filter.test(new Bean());
		Assert.assertTrue(test);
	}
	
	@Test
	public void test2() {
		Filter<Object> filter = OQLFilterBuilder.getFilter("property1=prop1 and bean1.property1=prop1");
		boolean test = filter.test(new Bean());
		Assert.assertTrue(test);
	}
	
	@Test
	public void test3() {
		Filter<Object> filter = OQLFilterBuilder.getFilter("property1=prop1 and bean1.property1=prop1 and map1.property2=prop2");
		boolean test = filter.test(new Bean());
		Assert.assertTrue(test);
	}
	
	@Test
	public void test4() {
		Filter<Object> filter = OQLFilterBuilder.getFilter("property1=wrongValue and bean1.property1=prop1 and map1.property2=prop2");
		boolean test = filter.test(new Bean());
		Assert.assertFalse(test);
	}

	@Test
	public void test5() {
		Filter<Object> filter = OQLFilterBuilder.getFilter("map1.wrongProperty=prop2");
		boolean test = filter.test(new Bean());
		Assert.assertFalse(test);
	}
	
	@Test
	public void test6() {
		Filter<Object> filter = OQLFilterBuilder.getFilter("");
		boolean test = filter.test(new Bean());
		Assert.assertTrue(test);
	}
	
	@Test
	public void test7() {
		Filter<Object> filter = OQLFilterBuilder.getFilter(null);
		boolean test = filter.test(new Bean());
		Assert.assertTrue(test);
	}
	
	@Test
	public void test8() {
		Filter<Object> filter = OQLFilterBuilder.getFilter("not(property1=prop1)");
		boolean test = filter.test(new Bean());
		Assert.assertFalse(test);
	}
	
	@Test
	public void test9() {
		Filter<Object> filter = OQLFilterBuilder.getFilter("not(property1=prop1) or bean1.property1=prop1");
		boolean test = filter.test(new Bean());
		Assert.assertTrue(test);
	}
	
	@Test
	public void test10() {
		Filter<Object> filter = OQLFilterBuilder.getFilter("property1=\"prop1\"");
		boolean test = filter.test(new Bean());
		Assert.assertTrue(test);
	}
	
	@Test
	public void test11() {
		Filter<Object> filter = OQLFilterBuilder.getFilter("property2=\"prop with some \"\"\"\"\"");
		boolean test = filter.test(new Bean());
		Assert.assertTrue(test);
	}
}
