/*******************************************************************************
 * Copyright 2021 exense GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package step.core.dynamicbeans;

import org.junit.Test;
import step.core.dynamicbeans.ValueConverter;

import static org.junit.Assert.assertEquals;

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
