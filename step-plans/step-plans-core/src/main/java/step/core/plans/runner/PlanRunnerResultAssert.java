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
package step.core.plans.runner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.junit.Assert;

public class PlanRunnerResultAssert {

	public static void assertEquals(File expectedFile, File actualFile) throws IOException {
		Assert.assertEquals(readResource(expectedFile), readResource(actualFile));
	}
	
	/**
	 * Compare 2 resources
	 * @param clazz the class loader of the expected resource
	 * @param expectedResourceName the name of the expected resource
	 * @param actualFile the actual file
	 * @param ignoredPatterns the regexp patterns to be ignored during comparison
	 * @throws IOException
	 */
	public static void assertEquals(Class<?> clazz, String expectedResourceName, File actualFile, String... ignoredPatterns) throws IOException {
		String expected = readResource(clazz, expectedResourceName);
		expected = removeIgnoredPatterns(expected, ignoredPatterns);
		
		String actual = readResource(actualFile);
		actual = removeIgnoredPatterns(actual, ignoredPatterns);
		
		Assert.assertEquals(expected, actual);
	}

	protected static String removeIgnoredPatterns(String text, String... ignoredPatterns) {
		String result = text;
		for(String pattern:ignoredPatterns) {
			result = text.replaceAll(pattern, "");
		}
		return result;
	}
	
	public static void assertEquals(String expectedReportTree, PlanRunnerResult actualResult) throws IOException {
		StringWriter writer = new StringWriter();
		actualResult.printTree(writer);
		Assert.assertEquals(normalizeEndOfLines(expectedReportTree), normalizeEndOfLines(writer.toString()));
	}

	protected static String normalizeEndOfLines(String expectedReportTree) {
		return expectedReportTree.replaceAll("\r\n", "\n");
	}
	
	public static void assertEquals(File expectedReportTreeFile, PlanRunnerResult actualResult) throws IOException {
		StringWriter writer = new StringWriter();
		actualResult.printTree(writer);
		Assert.assertEquals(readResource(expectedReportTreeFile), writer.toString());
	}
	
	public static void assertEquals(Class<?> clazz, String resourceName, PlanRunnerResult actualResult) throws IOException {
		StringWriter writer = new StringWriter();
		actualResult.printTree(writer);
		Assert.assertEquals(readResource(clazz, resourceName), writer.toString());
	}
	
	public static String readResource(Class<?> clazz, String resourceName) {
		return readStream(clazz.getResourceAsStream(resourceName));
	}

	public static String readResource(File resource) throws FileNotFoundException, IOException {
		try(FileInputStream fis = new FileInputStream(resource)){
			return readStream(fis);
		}
	}

	public static String readStream(InputStream is){
		try(Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
			return scanner.useDelimiter("\\A").next().replaceAll("\r\n", "\n");
		}
	}
}
