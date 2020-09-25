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
		try(AnnotationScanner annotationScanner = AnnotationScanner.forAllClassesFromContextClassLoader()) {
			Class<?> class1 = annotationScanner.getClassesWithAnnotation((TestAnnotation.class)).stream().findFirst().get();
			assertEquals(TestClass.class, class1);
			
			Method method1 = annotationScanner.getMethodsWithAnnotation(TestAnnotation.class).stream().findFirst().get();
			assertEquals("testMethod", method1.getName());
		}
	}

	@Test
	public void test2() {
		try(AnnotationScanner annotationScanner = AnnotationScanner.forAllClassesFromClassLoader(this.getClass().getClassLoader())) {
			Class<?> class1 = annotationScanner.getClassesWithAnnotation((TestAnnotation.class)).stream().findFirst().get();
			assertEquals(TestClass.class, class1);
			
			Method method1 = annotationScanner.getMethodsWithAnnotation(TestAnnotation.class).stream().findFirst().get();
			assertEquals("testMethod", method1.getName());
		}
	}
	
	@Test
	public void test3() {
		try(AnnotationScanner annotationScanner = AnnotationScanner.forAllClassesFromClassLoader("step", this.getClass().getClassLoader())) {
			Class<?> class1 = annotationScanner.getClassesWithAnnotation((TestAnnotation.class)).stream().findFirst().get();
			assertEquals(TestClass.class, class1);
			
			Method method1 = annotationScanner.getMethodsWithAnnotation(TestAnnotation.class).stream().findFirst().get();
			assertEquals("testMethod", method1.getName());
		}
	}
	
	@Test
	public void testAnnotationScannerForSpecificJars() {
		File file = FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(), "step-core-model-test.jar");
		try(AnnotationScanner annotationScanner = AnnotationScanner.forSpecificJar(file)) {
			List<Method> methods = annotationScanner.getMethodsWithAnnotation(ContainsDynamicValues.class).stream().collect(Collectors.toList());
			assertEquals(1, methods.size());
			assertEquals("testMethod", methods.get(0).getName());
		}
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
	
	@TestAnnotation
	public static class TestClass {

		@TestAnnotation
		public void testMethod() {
		}

	}
}
