/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.expressions;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import junit.framework.Assert;

public class ExpressionHandlerTest {

	@Test
	public void test() {
		ExpressionHandler e = new ExpressionHandler();
		Assert.assertEquals("abc",e.evaluate("ab[['c']]", null));
	}
	
	@Test
	public void test2() {
		ExpressionHandler e = new ExpressionHandler();
		Assert.assertEquals("abcd",e.evaluate("ab[['c']][['d']]", null));
	}
	
	@Test
	public void test3() {
		ExpressionHandler e = new ExpressionHandler();
		Assert.assertEquals("abcde",e.evaluate("ab[['c']][['d']]e", null));
	}
	
	@Test
	public void test4() {
		ExpressionHandler e = new ExpressionHandler();
		Assert.assertEquals("abcd",e.evaluate("ab[[if(true){return 'c'}]]d", null));
	}
	
	@Test
	public void test5() {
		Map<String, Object> o = new HashMap<>();
		o.put("var", "c");
		ExpressionHandler e = new ExpressionHandler();
		Assert.assertEquals("abcd",e.evaluate("ab[[var]]d", o));
	}

}
