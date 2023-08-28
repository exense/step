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
package step.plugins.java.handler;

import java.net.URLClassLoader;
import java.util.Arrays;

import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

public class MyKeywordLibrary extends AbstractKeyword {

	@Keyword
	public void MyKeyword1() {
		output.add("MyKey", "MyValue");
		if(properties!=null) {
			properties.forEach((key, value)->{
				output.add(key, value);
			});
		}
	}

	@Keyword(timeout=100)
	public void MyKeywordWithTimeout() {
		output.add("MyKey", "MyValue");
		if(properties!=null) {
			properties.forEach((key, value)->{
				output.add(key, value);
			});
		}
	}

	@Keyword(schema = "{\n" +
			"  \"properties\": {\n" +
			"    \"number\": {\n" +
			"      \"type\": \"number\",\n" +
			"      \"default\": 1.1\n" +
			"    },\n" +
			"    \"boolean\": {\n" +
			"      \"type\": \"boolean\",\n" +
			"      \"default\": true\n" +
			"    },\n" +
			"    \"enum\": {\n" +
			"      \"enum\": [\n" +
			"        \"val1\",\n" +
			"        \"val2\",\n" +
			"        \"val3\"\n" +
			"      ],\n" +
			"      \"default\": \"val1\"\n" +
			"    },\n" +
			"    \"text\": {\n" +
			"      \"type\": \"string\",\n" +
			"      \"default\": \"some text input\"\n" +
			"    },\n" +
			"    \"array\": {\n" +
			"      \"type\": \"array\",\n" +
			"      \"default\": [\n" +
			"        \"val1\",\n" +
			"        \"val2\",\n" +
			"        \"val3\"\n" +
			"      ]\n" +
			"    },\n" +
			"    \"object\": {\n" +
			"      \"type\": \"object\",\n" +
			"      \"default\": {\n" +
			"        \"key1\": \"val1\"\n" +
			"      }\n" +
			"    }\n" +
			"  },\n" +
			"  \"required\": [\n" +
			"    \"number\",\n" +
			"    \"boolean\",\n" +
			"    \"enum\",\n" +
			"    \"text\",\n" +
			"    \"array\",\n" +
			"    \"object\"\n" +
			"  ]\n" +
			"}")
	public void MyKeywordCopyDefaultValues() {
		output.setPayloadJson(input.toString());
	}
	
	@Keyword
	public void TestClassloader() {
		// the context classloader should be equal to the class loader of the keyword as many framework rely 
		// on context class loader lookup
		ClassLoader contextClassloader = Thread.currentThread().getContextClassLoader();
		try {
			assert contextClassloader instanceof URLClassLoader;
			output.add("clURLs", Arrays.toString(((URLClassLoader)contextClassloader).getURLs()));
		} catch(Exception e) {
			throw new AssertionError("Context CL was not an URLClassloader as expected but was: "+contextClassloader, e);
		}
	}
}
