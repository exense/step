package step.functions.packages.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import ch.exense.commons.io.FileHelper;
import step.core.accessors.AbstractOrganizableObject;
import step.functions.Function;
import step.functions.packages.handlers.FunctionPackageUtils.DiscovererParameters;

public class JavaFunctionPackageDaemonTest {

	@Test
	public void test() {
		File testResource = FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(), "local/java-plugin-handler-test.jar");
		JavaFunctionPackageDaemon daemon = new JavaFunctionPackageDaemon();;
		DiscovererParameters discovererParameters = new DiscovererParameters();
		discovererParameters.setPackageLocation(testResource.getAbsolutePath());
		FunctionList functions = daemon.getFunctions(discovererParameters);
		assertEquals(6, functions.getFunctions().size());

		Function timeout = functions.getFunctions().stream().filter(f -> f.getAttribute(AbstractOrganizableObject.NAME).equals("MyKeywordWithTimeout")).findFirst().get();
		assertEquals(100L,(long) timeout.getCallTimeout().get());

		Function myKeywordWithRoutingToController = functions.getFunctions().stream().filter(f -> f.getAttribute(AbstractOrganizableObject.NAME).equals("MyKeywordWithRoutingToController")).findFirst().get();
		assertTrue(myKeywordWithRoutingToController.isExecuteLocally());

		Function myKeywordWithRoutingCriteria = functions.getFunctions().stream().filter(f -> f.getAttribute(AbstractOrganizableObject.NAME).equals("MyKeywordWithRoutingCriteria")).findFirst().get();
		Map<String, String> expectedRoutingCriteria = Map.of("OS", "WINDOWS", "TYPE", "PLAYWRIGHT");
		assertEquals(expectedRoutingCriteria, myKeywordWithRoutingCriteria.getTokenSelectionCriteria());
	}
}
