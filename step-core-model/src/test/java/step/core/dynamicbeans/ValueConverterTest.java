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
