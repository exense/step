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
package step.repositories.parser;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import step.expressions.ExpressionHandler;

public class BaseScriptTest {
	
	ExpressionHandler h = new ExpressionHandler("step.repositories.parser.BaseScript");

	@Test
	public void test() {
		Object s = h.evaluateGroovyExpression("heute()", null);
		Assert.assertEquals(format(new Date(), "dd.MM.yyyy"), s);
		
		s = h.evaluateGroovyExpression("jetzt()", null);
		Assert.assertEquals(format(new Date(), "dd.MM.yyyy HH:mm:ss"), s);
		
		s = h.evaluateGroovyExpression("datum('1.minute.from.now')", null);
		
		s = h.evaluateGroovyExpression("letzterTagImMonat()", null);
		s = h.evaluateGroovyExpression("letzterTagImMonat('dd')", null);
		
		s = h.evaluateGroovyExpression("ersterTagNaechsterMonat('dd.MM.yyyy')", null);
		
		s = h.evaluateGroovyExpression("ersterTagImMonat('dd.MM.yyyy')", null);
	}
	
	private String format(Date date, String format) {
		SimpleDateFormat f = new SimpleDateFormat(format);
		return f.format(date);

	}
}
