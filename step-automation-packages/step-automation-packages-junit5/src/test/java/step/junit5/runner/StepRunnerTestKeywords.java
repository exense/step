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
package step.junit5.runner;

import org.junit.Assert;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Input;
import step.handlers.javahandler.Keyword;

import java.util.List;
import java.util.Map;

public class StepRunnerTestKeywords extends AbstractKeyword {

	@Keyword
	public void callExisting() {
		System.out.println("My first keyword is called!");
	}

	@Keyword
	public void callExisting2() {
		System.out.println("My second keyword is called from composite!");
	}

	@Keyword
	public void callExisting3(@Input(name = "stringInput") String stringInput, @Input(name = "intInput") Integer intInput) {
		System.out.println("My third keyword is called with inputs: " + stringInput + ", " + intInput);

		// assert input values used in tests
		Assert.assertEquals("abc", stringInput);
		Assert.assertEquals(Integer.valueOf(777), intInput);
	}

	@Keyword(planReference = "plans/composite1.plan", schema = "{ \"properties\": { "
			+ "\"myInput\": {\"type\": \"string\", \"default\":\"defaultValueString\"}"
			+ "}, \"required\" : []}", properties = { "" },
			description = "Keyword used to explicitly close the current driver.", name="MyCompoInPackage")
	public void composite1() {

	}

	@Keyword
	public void customKeywordWithMapInput(@Input(name = "URL") String url,
										  @Input(name = "Name") String name,
										  @Input(name = "Headers") Map<String, String> headers,
										  @Input(name = "ArrayInput") List<Integer> arrayInput
	){
		System.out.println("Custom keyword with map input has been called");
		System.out.println("URL: " + url);
		System.out.println("Name: " + name);
		System.out.println("Headers: " + headers);
		System.out.println("ArrayInput: " + arrayInput);
	}
}
