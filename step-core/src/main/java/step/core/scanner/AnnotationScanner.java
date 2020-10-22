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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

public class AnnotationScanner implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(AnnotationScanner.class);

	private final ScanResult scanResult;
	private final ClassLoader classLoader;

	private AnnotationScanner(ScanResult scanResult, ClassLoader classLoader) {
		this.scanResult = scanResult;
		this.classLoader = classLoader;
	}

	/**
	 * @return an instance of {@link AnnotationScanner} scanning all classes of the
	 *         context class loader
	 */
	public static AnnotationScanner forAllClassesFromContextClassLoader() {
		return forAllClassesFromClassLoader(null, Thread.currentThread().getContextClassLoader());
	}

	/**
	 * @param classloader the {@link ClassLoader} (including parents) to be scanned
	 * @return an instance of {@link AnnotationScanner} scanning all classes of the
	 *         provided class loader
	 */
	public static AnnotationScanner forAllClassesFromClassLoader(ClassLoader classloader) {
		return forAllClassesFromClassLoader(null, classloader);
	}

	/**
	 * @param packagePrefix the specific package to be scanned
	 * @param classloader   the {@link ClassLoader} (including parents) to be
	 *                      scanned
	 * @return an instance of {@link AnnotationScanner} scanning all classes of the
	 *         provided class loader
	 */
	public static AnnotationScanner forAllClassesFromClassLoader(String packagePrefix, ClassLoader classloader) {
		ClassGraph classGraph = new ClassGraph();
		if (packagePrefix != null) {
			classGraph.whitelistPackages(packagePrefix);
		}
		
		// In this method we would actually like to only scan the classes of the provided
		// class loader and thus use the following method. This method is unfortunately 
		// not working under Java 9+ (https://github.com/classgraph/classgraph/issues/382)
		//classGraph.overrideClassLoaders(classloader);
		classGraph.addClassLoader(classloader);
		classGraph.enableClassInfo().enableAnnotationInfo().enableMethodInfo();

		return scan(classGraph, classloader);
	}

	/**
	 * @param jar the specific jar file to be scanned
	 * @return an instance of {@link AnnotationScanner} scanning all classes of the
	 *         provided jar file
	 */
	public static AnnotationScanner forSpecificJar(File jar) {
		try {
			return forSpecificJar(jar,new URLClassLoader(new URL[] { jar.toURI().toURL() }));
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static AnnotationScanner forSpecificJar(File jar, ClassLoader classLoaderForResultClassesAndMethods) {
		URLClassLoader urlClassLoader;
		try {
			urlClassLoader = new URLClassLoader(new URL[] { jar.toURI().toURL() });
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return forSpecificJarFromURLClassLoader(urlClassLoader,classLoaderForResultClassesAndMethods);
	}

	/**
	 * Scans the jar files of a specific {@link URLClassLoader}
	 * 
	 * @param classloader the specific {@link ClassLoader} to scan the {@link URL}s
	 *                    of
	 * @return an instance of {@link AnnotationScanner} scanning all classes of the
	 *         provided {@link URLClassLoader} (parent excluded)
	 */
	public static AnnotationScanner forSpecificJarFromURLClassLoader(URLClassLoader classloader) {
		return forSpecificJarFromURLClassLoader(classloader,classloader);
	}

	/**
	 * Scans the jar files of a specific {@link URLClassLoader}
	 *
	 * @param classloader the specific {@link ClassLoader} to scan the {@link URL}s
	 * @param classLoaderForResultClassesAndMethods the {@link ClassLoader} containing the context
	 *
	 * @return an instance of {@link AnnotationScanner} scanning all classes of the
	 *         provided {@link URLClassLoader} (parent excluded)
	 */
	public static AnnotationScanner forSpecificJarFromURLClassLoader(URLClassLoader classloader, ClassLoader classLoaderForResultClassesAndMethods) {
		List<String> jars = Arrays.asList(classloader.getURLs()).stream().map(url -> {
			try {
				// Use url decoder to ensure that escaped space %20 are unescaped properly
				return URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8.name());
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());

		ClassGraph classGraph = new ClassGraph().overrideClasspath(jars).enableClassInfo().enableAnnotationInfo()
				.enableMethodInfo();

		return scan(classGraph, classLoaderForResultClassesAndMethods);
	}

	private static AnnotationScanner scan(ClassGraph classGraph, ClassLoader classLoaderForResultClassesAndMethods) {
		long t1 = System.currentTimeMillis();
		logger.info("Scanning classpath...");
		ScanResult scanResult = classGraph.scan();
		logger.info("Scanned classpath in " + (System.currentTimeMillis() - t1) + "ms");
		AnnotationScanner annotationScanner = new AnnotationScanner(scanResult, classLoaderForResultClassesAndMethods);
		return annotationScanner;
	}

	/**
	 * Get all classes annotated by the provided {@link Annotation}
	 * 
	 * @param annotationClass
	 * @return the {@link Set} of classes annotated by the provided
	 *         {@link Annotation}
	 */
	public Set<Class<?>> getClassesWithAnnotation(Class<? extends Annotation> annotationClass) {
		ClassInfoList classInfos = scanResult.getClassesWithAnnotation(annotationClass.getName());
		return loadClassesFromClassInfoList(classLoader, classInfos);
	}

	/**
	 * Get all methods annotated by the provided {@link Annotation}
	 * 
	 * @param annotationClass
	 * @return the {@link Set} of methods annotated by the provided
	 *         {@link Annotation}
	 */
	public Set<Method> getMethodsWithAnnotation(Class<? extends Annotation> annotationClass) {
		Set<Method> result = new HashSet<>();
		ClassInfoList classInfos = scanResult.getClassesWithMethodAnnotation(annotationClass.getName());
		Set<Class<?>> classesFromClassInfoList = loadClassesFromClassInfoList(classLoader, classInfos);
		classesFromClassInfoList.forEach(c -> {
			Method[] methods = c.getMethods();
			for (Method method : methods) {
				if (isAnnotationPresent(annotationClass, method)) {
					result.add(method);
				}
			}
		});
		return result;
	}

	/**
	 * Alternative implementation of {@link Class#isAnnotationPresent(Class)} which
	 * doesn't rely on class equality but class names. The class loaders of the
	 * annotationClass and the method provided as argument might be different
	 * 
	 * @param annotationClass
	 * @param method
	 * @return
	 */
	private static boolean isAnnotationPresent(Class<? extends Annotation> annotationClass, Method method) {
		return Arrays.asList(method.getAnnotations()).stream()
				.filter(an -> an.annotationType().getName().equals(annotationClass.getName())).findAny().isPresent();
	}

	private static Set<Class<?>> loadClassesFromClassInfoList(ClassLoader classloader, ClassInfoList classInfos) {
		return classInfos.getNames().stream().map(Classes.loadWith(classloader)).collect(Collectors.toSet());
	}

	@Override
	public void close() {
		scanResult.close();
	}
}
