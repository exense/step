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
package step.repositories.parser.keyvalue;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import step.repositories.parser.keyvalue.KeyValueParser.KeyValueContext;
import step.repositories.parser.keyvalue.KeyValueParser.ParseContext;

public class KeyValueParserTest {

	@Test
	public void test() {
		StringBuilder result = new StringBuilder();
		StringBuilder dynamicExpressions = new StringBuilder();

		KeyValueParser p = new KeyValueParser(new CommonTokenStream(new KeyValueLexer(new ANTLRInputStream("k1 = | a||b | k2 = \"myString \"    k3=myVar k4=|'test'|"))));
		
		ParseContext context = p.parse();
		KeyValueVisitor<Object> visitor = new KeyValueBaseVisitor<Object>() {

			@Override
			public Object visitKeyValue(KeyValueContext ctx) {
				result.append(ctx.value().getText());
				
				if(ctx.value().DYNAMIC_EXPRESSION()!=null) {
					dynamicExpressions.append(ctx.value().DYNAMIC_EXPRESSION().getText()).append(";");
				}
				
				return super.visitKeyValue(ctx);
			}
		};
		visitor.visit(context);
		
		p.parse();
		
		Assert.assertEquals("| a||b |\"myString \"myVar|'test'|", result.toString());
		Assert.assertEquals("| a||b |;|'test'|;", dynamicExpressions.toString());

	}

}
