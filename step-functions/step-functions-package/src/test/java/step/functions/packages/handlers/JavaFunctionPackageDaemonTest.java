package step.functions.packages.handlers;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import ch.exense.commons.io.FileHelper;
import step.functions.packages.handlers.FunctionPackageUtils.DiscovererParameters;

public class JavaFunctionPackageDaemonTest {

	@Test
	public void test() {
		File testResource = FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(), "TestResource.jar");
		JavaFunctionPackageDaemon daemon = new JavaFunctionPackageDaemon();;
		DiscovererParameters discovererParameters = new DiscovererParameters();
		discovererParameters.setPackageLocation(testResource.getAbsolutePath());
		FunctionList functions = daemon.getFunctions(discovererParameters);
		assertEquals(2, functions.getFunctions().size());
	}
}
