package step.core.plans.runner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import junit.framework.Assert;

public class PlanRunnerResultAssert {

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
