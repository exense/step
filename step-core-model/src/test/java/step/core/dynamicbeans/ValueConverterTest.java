package step.core.dynamicbeans;

import static org.junit.Assert.*;

import org.junit.Test;

public class ValueConverterTest {

	@Test
	public void test() {
		Object value;
		
		// string to long
		value = ValueConverter.convert("1", Long.class);
		assertEquals(Long.class, value.getClass());
		assertEquals(1, (long) value);
		
		// long to long
		value = ValueConverter.convert(1l, Long.class);
		assertEquals(Long.class, value.getClass());
		assertEquals(1, (long) value);
		
		// int to long
		value = ValueConverter.convert(1, Long.class);
		assertEquals(Long.class, value.getClass());
		assertEquals(1, (long) value);
		
		// double to long
		value = ValueConverter.convert(1.1, Long.class);
		assertEquals(Long.class, value.getClass());
		assertEquals(1, (long) value);
		
		// string to int
		value = ValueConverter.convert("1", Integer.class);
		assertEquals(Integer.class, value.getClass());
		assertEquals(1, (int) value);
		
		// int to int
		value = ValueConverter.convert(1, Integer.class);
		assertEquals(Integer.class, value.getClass());
		assertEquals(1, (int) value);
		
		// string to int
		value = ValueConverter.convert("1", String.class);
		assertEquals(String.class, value.getClass());
		assertEquals("1", value);
	}
}
