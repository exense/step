package step.core.scanner;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import ch.exense.commons.io.FileHelper;
import step.core.dynamicbeans.ContainsDynamicValues;

public class AnnotationScannerTest {

	@Test
	public void test() {
		Class<?> class1 = AnnotationScanner.getClassesWithAnnotation(TestAnnotation.class).stream().findFirst().get();
		assertEquals(TestClass.class, class1);
	}

	@Test
	public void test2() {
		Class<?> class1 = AnnotationScanner
				.getClassesWithAnnotation(TestAnnotation.class, this.getClass().getClassLoader()).stream().findFirst()
				.get();
		assertEquals(TestClass.class, class1);
	}

	@Test
	public void test3() {
		Class<?> class1 = AnnotationScanner
				.getClassesWithAnnotation("step", TestAnnotation.class, this.getClass().getClassLoader()).stream()
				.findFirst().get();
		assertEquals(TestClass.class, class1);
	}

	@Test
	public void testMethod1() {
		Method method1 = AnnotationScanner.getMethodsWithAnnotation(TestAnnotation.class).stream().findFirst().get();
		assertEquals("testMethod", method1.getName());
	}

	@Test
	public void testMethod2() {
		Method method1 = AnnotationScanner
				.getMethodsWithAnnotation(TestAnnotation.class, this.getClass().getClassLoader()).stream().findFirst()
				.get();
		assertEquals("testMethod", method1.getName());
	}

	@Test
	public void testMethod3() {
		Method method1 = AnnotationScanner
				.getMethodsWithAnnotation("step", TestAnnotation.class, this.getClass().getClassLoader()).stream()
				.findFirst().get();
		assertEquals("testMethod", method1.getName());
	}
	
	@Test
	public void testGetMethodsWithAnnotation() {
		File file = FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(), "step-core-model-test.jar");
		List<Method> methods = AnnotationScanner.getMethodsWithAnnotation(ContainsDynamicValues.class, file).stream().collect(Collectors.toList());
		assertEquals(1, methods.size());
		assertEquals("testMethod", methods.get(0).getName());
	}

	// Don't remove this class
	// It is here to ensure that annotation scanning performed in
	// testGetMethodsWithAnnotation() isn't finding other methods that the 
	// one contained in the specified jar step-core-model-test.jar
	public static class TestBean {
		
		@ContainsDynamicValues
		public void testMethod2() {
			// This method 
		}
		
	}
	
	@Test
	public void testClearCache() {
		Method method1 = AnnotationScanner.getMethodsWithAnnotation(TestAnnotation.class).stream().findFirst().get();
		assertEquals("testMethod", method1.getName());
		AnnotationScanner.clearCache();
		method1 = AnnotationScanner.getMethodsWithAnnotation(TestAnnotation.class).stream().findFirst().get();
		assertEquals("testMethod", method1.getName());
	}

	
	@TestAnnotation
	public static class TestClass {

		@TestAnnotation
		public void testMethod() {
		}

	}
}
