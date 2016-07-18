package step.commons.buffering;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class FilterIteratorTest {
	
	@Test
	public void test() {
		List<String> testList = new ArrayList<String>();
		testList.add("a");		
		testList.add("b");
		testList.add("b");
		testList.add("a");
		testList.add("b");
		testList.add("a");
		testList.add("b");
		
		ObjectFilter<String> filter = new ObjectFilter<String>() {
			@Override
			public boolean matches(String o) {
				return o.equals("a");
			}
		};
		
		FilterIterator<String> f = new FilterIterator<String>(testList.iterator(), filter);
		List<String> result = new ArrayList<>();
		
		while(f.hasNext()) {
			result.add(f.next());
		}
		
		Assert.assertEquals("Result size after filtering", 3, result.size());	
	}
	
	@Test
	public void testNoMatch() {
		List<String> testList = new ArrayList<String>();
		testList.add("a");		
		testList.add("b");
		testList.add("b");
		testList.add("a");
		testList.add("b");
		testList.add("a");
		testList.add("b");
		
		ObjectFilter<String> filter = new ObjectFilter<String>() {
			@Override
			public boolean matches(String o) {
				return o.equals("c");
			}
		};
		
		FilterIterator<String> f = new FilterIterator<String>(testList.iterator(), filter);
		List<String> result = new ArrayList<>();
		
		while(f.hasNext()) {
			result.add(f.next());
		}
		
		Assert.assertEquals("Result size after filtering", 0, result.size());	
	}
	
	@Test
	public void testEmptyList() {
		List<String> testList = new ArrayList<String>();
		
		ObjectFilter<String> filter = new ObjectFilter<String>() {
			@Override
			public boolean matches(String o) {
				return o.equals("c");
			}
		};
		
		FilterIterator<String> f = new FilterIterator<String>(testList.iterator(), filter);
		List<String> result = new ArrayList<>();
		
		while(f.hasNext()) {
			result.add(f.next());
		}
		
		Assert.assertEquals("Result size after filtering", 0, result.size());	
	}
}
