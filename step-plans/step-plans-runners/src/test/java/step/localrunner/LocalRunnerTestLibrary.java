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
package step.localrunner;

import junit.framework.Assert;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

public class LocalRunnerTestLibrary extends AbstractKeyword {

	@Keyword
	public void keyword1() {
		output.setPayloadJson(input.toString());
		System.out.println("Keyword1!");
	}
	
	@Keyword
	public void keyword2() {
		output.add("Att2", "Val2");
		System.out.println("Keyword2!"+input.getString("Param1"));
	}
	
	@Keyword
	public void writeSessionValue() {
		session.put(input.getString("key"), input.getString("value"));
	}
	
	@Keyword
	public void readSessionValue() {
		output.add(input.getString("key"), session.get(input.getString("key")).toString());
	}
	
	@Keyword
	public void assertSessionValue() {
		Assert.assertEquals(input.getString("value"), session.get(input.getString("key"))!=null?session.get(input.getString("key")).toString():"");
	}
}
