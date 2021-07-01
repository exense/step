package step.functions.packages;

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
import ch.exense.commons.io.FileHelper;
import step.attachments.FileResolver;
import step.core.accessors.AbstractOrganizableObject;
import step.functions.Function;
import step.functions.manager.FunctionManager;
import step.functions.packages.handlers.JavaFunctionPackageHandler;
import step.functions.type.FunctionTypeException;
import step.resources.InMemoryResourceAccessor;
import step.resources.InMemoryResourceRevisionAccessor;
import step.resources.LocalResourceManagerImpl;
import step.resources.Resource;
import step.resources.ResourceAccessor;
import step.resources.ResourceManager;
import step.resources.ResourceManagerImpl;
import step.resources.ResourceRevisionAccessor;
import step.resources.SimilarResourceExistingException;

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
				resolver,new Configuration());
	}
	
	@After
	public void after() throws IOException {
		pm.close();
	}
	
	@Test
	public void test() throws Exception {
		Map<String, String> functionAttributes = new HashMap<>();
		functionAttributes.put("att1", "valInitial");
		functionAttributes.put("att2", "val2Initial");

		Function f1 = function("f1");
		Function f2 = function("f2");
		
		final List<Function> packageManagerFunctions = new ArrayList<>();
		packageManagerFunctions.add(f1);
		packageManagerFunctions.add(f2);
		pm.registerFunctionPackageHandler(new FunctionPackageHandler() {
			@Override
			public List<Function> buildFunctions(FunctionPackage functionPackage, boolean preview) throws Exception {
				return packageManagerFunctions;
			}
			
			@Override
			public boolean isValidForPackage(FunctionPackage functionPackag) {
				return true;
			}
		});

		FunctionPackage fp = new FunctionPackage();
		fp.setId(new ObjectId());
		Map<String, String> a = new HashMap<>();
		a.put("att1", "val1Fp");
		fp.setPackageAttributes(a);
		fp.setPackageLocation("testLocation.test");

		pm.addOrUpdateFunctionPackage(fp, null);

		Function f1Repo = f.getFunctionById(f1.getId().toString());
		Assert.assertEquals("val1Fp", f1Repo.getAttributes().get("att1"));
		Assert.assertEquals("val2Initial", f1Repo.getAttributes().get("att2"));
		Assert.assertEquals(f1, f1Repo);
		Assert.assertEquals(f2, f.getFunctionById(f2.getId().toString()));

		f1.getAttributes().put("att3", "newVal");

		Function f3 = function("f3");

		packageManagerFunctions.clear();
		packageManagerFunctions.add(f1);
		packageManagerFunctions.add(f3);

		pm.addOrUpdateFunctionPackage(fp, null);

		// assert that function2 has been deleted as it is not part of the package
		// anymore
		Assert.assertNull(f.getFunctionById(f2.getId().toString()));

		// assert that function3 has been added
		Function f3Repo = f.getFunctionById(f3.getId().toString());
		Assert.assertEquals("val1Fp", f3Repo.getAttributes().get("att1"));

		f1Repo = f.getFunctionById(f1.getId().toString());
		Assert.assertEquals("val1Fp", f1Repo.getAttributes().get("att1"));
		Assert.assertEquals("val2Initial", f1Repo.getAttributes().get("att2"));
		Assert.assertEquals("newVal", f1Repo.getAttributes().get("att3"));

		List<ObjectId> newIds = new ArrayList<>();
		newIds.add(f1.getId());
		newIds.add(f3.getId());
		Assert.assertEquals(newIds, fp.getFunctions());
	}

	protected Function function(String name) {
		Function f1 = new Function();
		f1.setId(new ObjectId());
		Map<String, String> functionAttributes = new HashMap<>();
		functionAttributes.put(AbstractOrganizableObject.NAME, name);
		functionAttributes.put("att1", "valInitial");
		functionAttributes.put("att2", "val2Initial");
		f1.setAttributes(functionAttributes);
		return f1;
	}

	@Test
	public void implicitLookupTest() throws Exception {
		String resourceFileName = "TestResource.jar";
		ResourceAccessor resourceAccessor = new InMemoryResourceAccessor();
		ResourceRevisionAccessor resourceRevisionAccessor = new InMemoryResourceRevisionAccessor();
		ResourceManager resourceManager = new ResourceManagerImpl(FileHelper.createTempFolder(), resourceAccessor, resourceRevisionAccessor);
		FunctionPackageAccessor p = new InMemoryFunctionPackageAccessorImpl();
		TestFunctionRepository f = new TestFunctionRepository();
		FileResolver resolver = new FileResolver(resourceManager);

		// Create a package
		try (FunctionPackageManager pm = new FunctionPackageManager(p, f, resourceManager, resolver, new Configuration())) {

			Resource testResource = createTestResource(resourceFileName, resourceManager);
			FunctionPackage testPackage = createTestPackage(testResource,pm,resolver);

			pm.addOrUpdateFunctionPackage(testPackage, null);

			// assert that package can be looked up by name
			Assert.assertEquals("val1Fp",pm.getPackageByResourceName(resourceFileName).getPackageAttributes().get("att1"));
		}
	}
	
	@Test
	public void consistentNamingTest() throws Exception {
		String resourceFileName = "TestResource.jar";

		// Create a package
		Resource testResource = createTestResource(resourceFileName, resourceManager);
		FunctionPackage testPackage = createTestPackage(testResource,pm,resolver);

		pm.addOrUpdateFunctionPackage(testPackage, null);
		// Package lookup
		FunctionPackage lookedup = pm.getPackageByResourceName(resourceFileName);
		// assert package is named after resource
		Assert.assertEquals(resourceFileName, lookedup.getAttributes().get(AbstractOrganizableObject.NAME));
		
		// Updating package with new resource (old resource should be deleted, so no collision on id)
		Resource newTestResource = createTestResource(resourceFileName, resourceManager);
		FunctionPackage newTestPackage = createTestPackage(newTestResource,pm,resolver);
		newTestPackage.setReferencePackageId(testPackage.getId().toString());
		pm.addOrUpdateFunctionPackage(newTestPackage, null);
		
		// Package lookup
		lookedup = pm.getPackageByResourceName(resourceFileName);
		// Name still the same
		Assert.assertEquals(resourceFileName, lookedup.getAttributes().get(AbstractOrganizableObject.NAME));
	}

	private Resource createTestResource(String resourceFileName, ResourceManager resourceManager) throws IOException, SimilarResourceExistingException {

		FileInputStream fis = new FileInputStream(new File("src/test/resources/" + resourceFileName));

		// Create a resource
		Resource resource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, fis,
				resourceFileName, false, null);
		Assert.assertNotNull(resource);
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
			pm.addOrUpdateFunctionPackage(functionPackage, null);
		} catch (Exception e) {
			actualException = e;
		}
		Assert.assertEquals("Unsupported package type", actualException.getMessage());
	}
}
