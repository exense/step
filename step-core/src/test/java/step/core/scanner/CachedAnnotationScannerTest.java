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

import java.lang.reflect.Method;

import org.junit.Test;

import step.core.scanner.AnnotationScannerTest.TestClass;

public class CachedAnnotationScannerTest {

	@Test
	public void test() {
		Class<?> class1 = CachedAnnotationScanner.getClassesWithAnnotation(TestAnnotation.class).stream().findFirst().get();
		assertEquals(TestClass.class, class1);
	}

	@Test
	public void test2() {
		Class<?> class1 = CachedAnnotationScanner
				.getClassesWithAnnotation(TestAnnotation.class, this.getClass().getClassLoader()).stream().findFirst()
				.get();
		assertEquals(TestClass.class, class1);
	}

	@Test
	public void test3() {
		Class<?> class1 = CachedAnnotationScanner
				.getClassesWithAnnotation("step", TestAnnotation.class, this.getClass().getClassLoader()).stream()
				.findFirst().get();
		assertEquals(TestClass.class, class1);
	}

	@Test
	public void testMethod1() {
		Method method1 = CachedAnnotationScanner.getMethodsWithAnnotation(TestAnnotation.class).stream().findFirst().get();
		assertEquals("testMethod", method1.getName());
	}

	@Test
	public void testMethod2() {
		Method method1 = CachedAnnotationScanner
				.getMethodsWithAnnotation(TestAnnotation.class, this.getClass().getClassLoader()).stream().findFirst()
				.get();
		assertEquals("testMethod", method1.getName());
	}

	@Test
	public void testMethod3() {
		Method method1 = CachedAnnotationScanner
				.getMethodsWithAnnotation("step", TestAnnotation.class, this.getClass().getClassLoader()).stream()
				.findFirst().get();
		assertEquals("testMethod", method1.getName());
	}
	
	@Test
	public void testClearCache() {
		Method method1 = CachedAnnotationScanner.getMethodsWithAnnotation(TestAnnotation.class).stream().findFirst().get();
		assertEquals("testMethod", method1.getName());
		CachedAnnotationScanner.clearCache();
		method1 = CachedAnnotationScanner.getMethodsWithAnnotation(TestAnnotation.class).stream().findFirst().get();
		assertEquals("testMethod", method1.getName());
	}
}
