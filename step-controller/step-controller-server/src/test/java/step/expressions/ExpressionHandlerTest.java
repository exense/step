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
package step.expressions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.junit.Assert;

import static junit.framework.Assert.*;

public class ExpressionHandlerTest {

	@Test
	public void testDefault() {
        Object o;
        try (ExpressionHandler e = new ExpressionHandler()) {
            o = e.evaluateGroovyExpression("1+1", null);
        }
		assertEquals(2,o);
	}
	
	@Test
	public void testBindings() {
        Object o;
        try (ExpressionHandler e = new ExpressionHandler("step.expressions.GroovyFunctions")) {
            Map<String, Object> b = new HashMap<>();
            b.put("test", "value");
            o = e.evaluateGroovyExpression("test", b);
        }
        assertEquals("value", o.toString());
	}
	
	@Test
	public void testScriptBaseClass() {
        Object o;
        try (ExpressionHandler e = new ExpressionHandler("step.expressions.GroovyFunctions")) {
            o = e.evaluateGroovyExpression("yyyyMMdd", null);
        }
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
		assertEquals(f.format(new Date()), o.toString());
	}

	@Test
	public void testFunction() {
        Object o;
        try (ExpressionHandler e = new ExpressionHandler("step.expressions.GroovyFunctions")) {
            o = e.evaluateGroovyExpression("IsEmpty(\"sts\")", null);
        }
        assertFalse((boolean) o);
	}

	@Test
	public void testScriptBaseClassWithArrays() {
        Object o;
        try (ExpressionHandler e = new ExpressionHandler("step.expressions.GroovyTestFunctions")) {
            o = e.evaluateGroovyExpression("\"${testArrays()[0]}\"", null);
        }
        assertEquals("foo", o.toString());
	}
}
