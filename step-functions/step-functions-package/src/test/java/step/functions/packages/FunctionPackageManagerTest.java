package step.functions.packages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.exense.commons.app.Configuration;
import step.attachments.FileResolver;
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectHookRegistry;
import step.functions.Function;
import step.functions.manager.FunctionManager;
import step.functions.packages.handlers.JavaFunctionPackageHandler;
import step.functions.type.FunctionTypeException;
import step.resources.*;

public class FunctionPackageManagerTest {

	private FunctionPackageManager pm;
	private TestFunctionRepository f;
	private FileResolver resolver;
	private LocalResourceManagerImpl resourceManager;
	
	@Before
	public void before() {
		FunctionPackageAccessor p = new InMemoryFunctionPackageAccessorImpl();
		f = new TestFunctionRepository();
		resourceManager = new LocalResourceManagerImpl();
		resolver = new FileResolver(resourceManager);
		pm = new FunctionPackageManager(p, f, resourceManager,
				resolver,new Configuration(), new ObjectHookRegistry());
	}
	
	@After
	public void after() throws IOException {
		pm.close();
	}
	
	@Test
	public void test() throws Exception {
		Function f1 = function("f1");
		Function f2 = function("f2");
		
		List<Function> packageManagerFunctions = new ArrayList<Function>();
		packageManagerFunctions.add(f1);
		packageManagerFunctions.add(f2);
		
		registerPackageHandler(packageManagerFunctions);

		FunctionPackage fp = new FunctionPackage();
		fp.setId(new ObjectId());
		Map<String, String> a = new HashMap<>();
		a.put("att1", "val1Fp");
		fp.setPackageAttributes(a);
		fp.setPackageLocation("testLocation.test");
		
		// Test the package preview
		List<Function> packagePreview = pm.getPackagePreview(fp);
		assertEquals(2, packagePreview.size());

		// Add the function package
		pm.addOrUpdateFunctionPackage(fp);

		// Retrieve the created package
		FunctionPackage actualFunctionPackage = pm.getFunctionPackage(fp.getId().toString());
		// Assert its name
		assertEquals("testLocation.test", actualFunctionPackage.getName());
		
		// Get the first function that should have been generated
		Function f1Repo = f.getFunctionById(f1.getId().toString());
		assertEquals("val1Fp", f1Repo.getAttribute("att1"));
		assertEquals("val2Initial", f1Repo.getAttribute("att2"));
		assertEquals(f1, f1Repo);
		assertEquals(f2, f.getFunctionById(f2.getId().toString()));
		assertTrue(f1Repo.isManaged());
		assertEquals(fp.getId().toString(), f1Repo.getCustomField(FunctionPackageEntity.FUNCTION_PACKAGE_ID));
		assertEquals(fp.isExecuteLocally(), f1Repo.isExecuteLocally());
		assertEquals(fp.getTokenSelectionCriteria(), f1Repo.getTokenSelectionCriteria());
		
		// Update the function 1
		f1.addAttribute("att3", "newVal");

		// Create a third function
		Function f3 = function("f3");

		packageManagerFunctions.clear();
		packageManagerFunctions.add(f1);
		packageManagerFunctions.add(f3);

		// Update the function package
		pm.addOrUpdateFunctionPackage(fp);

		// assert that function2 has been deleted as it is not part of the package
		// anymore
		assertNull(f.getFunctionById(f2.getId().toString()));

		// assert that function3 has been added
		Function f3Repo = f.getFunctionById(f3.getId().toString());
		assertEquals("val1Fp", f3Repo.getAttribute("att1"));

		// assert that function 1 has been updated
		f1Repo = f.getFunctionById(f1.getId().toString());
		assertEquals("val1Fp", f1Repo.getAttribute("att1"));
		assertEquals("val2Initial", f1Repo.getAttribute("att2"));
		assertEquals("newVal", f1Repo.getAttribute("att3"));

		List<ObjectId> newIds = new ArrayList<>();
		newIds.add(f1.getId());
		newIds.add(f3.getId());
		assertEquals(newIds, fp.getFunctions());
		
		packageManagerFunctions.clear();
		pm.reloadFunctionPackage(fp.getId().toString());
		List<Function> packageFunctions = pm.getPackageFunctions(fp.getId().toString());
		
		assertEquals(0, packageFunctions.size());
		
		// Remove the function package
		pm.removeFunctionPackage(fp.getId().toString());
		
		// Assert that the functions have been deleted
		assertNull(f.getFunctionById(f1.getId().toString()));
		assertNull(f.getFunctionById(f3.getId().toString()));
	}
	
	@Test
	public void testAttributeResolver() throws Exception {
		Function f1 = function("f1");
		registerPackageHandler(List.of(f1));

		FunctionPackage fp = new FunctionPackage();
		fp.setId(new ObjectId());
		fp.setPackageLocation("testLocation.test");
		fp.addAttribute("dynamicAttribute", "@test");

		// Register an attribute resolver
		pm.registerAttributeResolver("dynamicAttribute", t -> "ResolvedValue1");

		// Add the function package
		pm.addOrUpdateFunctionPackage(fp);

		// Retrieve the created package
		FunctionPackage actualFunctionPackage = pm.getFunctionPackage(fp.getId().toString());
		// Assert the value has been resolved
		assertEquals("ResolvedValue1", actualFunctionPackage.getAttribute("dynamicAttribute"));
		
		// Change to a constant value
		fp.addAttribute("dynamicAttribute", "ConstantValue1");
		
		// Update the function package
		pm.addOrUpdateFunctionPackage(fp);
		
		actualFunctionPackage = pm.getFunctionPackage(fp.getId().toString());
		// Assert that the value hasn't been resolved
		assertEquals("ConstantValue1", actualFunctionPackage.getAttribute("dynamicAttribute"));
		
	}
	
	@Test
	public void resourceBasedFunctionPackage() throws Exception {
		String resourceFileName = "TestResource.jar";

		Function f1 = function("f1");
		registerPackageHandler(List.of(f1));
		
		// Create a package
		Resource testResource = createTestResource(resourceFileName, resourceManager);
		FunctionPackage testPackage = createTestPackage(testResource,pm,resolver);
		// Add a library resource
		Resource libraryResource1 = createTestResource(resourceFileName, resourceManager);
		testPackage.setPackageLibrariesLocation(resolver.createPathForResourceId(libraryResource1.getId().toString()));

		pm.addOrUpdateFunctionPackage(testPackage);
		
		// Retrieve the created package
		FunctionPackage actualFunctionPackage = pm.getFunctionPackage(testPackage.getId().toString());
		// Assert its name
		assertEquals(resourceFileName, actualFunctionPackage.getName());
		
		// Get the first function that should have been generated
		Function f1Repo = f.getFunctionById(f1.getId().toString());
		assertNotNull(f1Repo);
		
		// Update the package with a new resource
		Resource testResource2 = createTestResource(resourceFileName, resourceManager);
		FunctionPackage testPackage2 = createTestPackage(testResource2,pm,resolver);
		testPackage2.setId(testPackage.getId());
		
		// update the library resource too
		Resource libraryResource2 = createTestResource(resourceFileName, resourceManager);
		testPackage2.setPackageLibrariesLocation(resolver.createPathForResourceId(libraryResource2.getId().toString()));
		
		pm.addOrUpdateFunctionPackage(testPackage2);
		
		// Assert that the old resource has been deleted
		Assert.assertThrows(RuntimeException.class, () -> resourceManager.getResource(testResource.getId().toString()));
		Assert.assertThrows(RuntimeException.class, () -> resourceManager.getResource(libraryResource1.getId().toString()));
		
		// Assert that the new resource still exist
		Resource actualResource2 = resourceManager.getResource(testResource2.getId().toString());
		assertNotNull(actualResource2);
		
		// Remove the package
		pm.removeFunctionPackage(testPackage2.getId().toString());
		
		// Assert that all resources have been deleted
		Assert.assertThrows(RuntimeException.class, () -> resourceManager.getResource(testResource2.getId().toString()));
		Assert.assertThrows(RuntimeException.class, () -> resourceManager.getResource(libraryResource2.getId().toString()));
	}

	private void registerPackageHandler(List<Function> packageManagerFunctions) {
		pm.registerFunctionPackageHandler(new FunctionPackageHandler() {
			@Override
			public List<Function> buildFunctions(FunctionPackage functionPackage, boolean preview, ObjectEnricher objectEnricher) throws Exception {
				return packageManagerFunctions;
			}
			
			@Override
			public boolean isValidForPackage(FunctionPackage functionPackag) {
				return true;
			}
		});
	}

	private Function function(String name) {
		Function f1 = new Function();
		f1.setId(new ObjectId());
		Map<String, String> functionAttributes = new HashMap<>();
		functionAttributes.put(AbstractOrganizableObject.NAME, name);
		functionAttributes.put("att1", "valInitial");
		functionAttributes.put("att2", "val2Initial");
		f1.setAttributes(functionAttributes);
		return f1;
	}

	private Resource createTestResource(String resourceFileName, ResourceManager resourceManager) throws Exception {
		FileInputStream fis = new FileInputStream(new File("src/test/resources/" + resourceFileName));

		// Create a resource
		Resource resource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, fis,
				resourceFileName, false, null);
		assertNotNull(resource);
		return resource;
	}

	private FunctionPackage createTestPackage(Resource resource, FunctionPackageManager pm, FileResolver resolver) {
		pm.registerFunctionPackageHandler(new JavaFunctionPackageHandler(resolver, null));

		FunctionPackage fp = new FunctionPackage();
		fp.setId(new ObjectId());
		Map<String, String> a = new HashMap<>();
		a.put("att1", "val1Fp");
		fp.setPackageAttributes(a);

		fp.setPackageLocation(FileResolver.RESOURCE_PREFIX + resource.getId().toString());

		return fp;
	}

	public static class TestFunctionRepository implements FunctionManager {

		Map<String, Function> m = new HashMap<>();

		@Override
		public Function getFunctionByAttributes(Map<String, String> attributes) {
			return null;
		}

		@Override
		public Function getFunctionById(String id) {
			return m.get(id);
		}

		@Override
		public Function saveFunction(Function function) {
			m.put(function.getId().toString(), function);
			return function;
		}

		@Override
		public void deleteFunction(String functionId) {
			m.remove(functionId);
		}

		@Override
		public Function copyFunction(String functionId) throws FunctionTypeException {
			return null;
		}

		@Override
		public Function newFunction(String functionType) {
			return null;
		}

	}
	
	@Test
	public void testUnknownPackageType() {
		FunctionPackage functionPackage = new FunctionPackage();
		functionPackage.setPackageLocation("WRONG PACKAGE LOCATION");
		Exception actualException = null;
		try {
			pm.addOrUpdateFunctionPackage(functionPackage);
		} catch (Exception e) {
			actualException = e;
		}
		assertEquals("Unsupported package type: WRONG PACKAGE LOCATION", actualException.getMessage());
	}
}
