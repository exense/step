package step.core.scanner;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

public class AnnotationScanner implements Closeable, AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(AnnotationScanner.class);
	private ScanResult scanResult;
	
	private static final ConcurrentHashMap<Key, AnnotationScanner> scanResults = new ConcurrentHashMap<>();
	
	private AnnotationScanner(ClassLoader classLoader, String... packagePrefixes) {
		super();
		long t1 = System.currentTimeMillis();
		logger.info("Scanning classpath...");
		ClassGraph classGraph = new ClassGraph().whitelistPackages(packagePrefixes).addClassLoader(classLoader).enableClassInfo().enableAnnotationInfo().enableMethodInfo();
		scanResult = classGraph.scan();
		logger.info("Scanned classpath in "+(System.currentTimeMillis()-t1)+"ms");
	}
	
	private static final String ALLPACKAGES_PREFIXES = "";

	public static Set<Class<?>> getClassesWithAnnotation(Class<? extends Annotation> annotationClass) {
		return getClassesWithAnnotation(annotationClass, Thread.currentThread().getContextClassLoader());
	}
	
	public static Set<Class<?>> getClassesWithAnnotation(Class<? extends Annotation> annotationClass, ClassLoader classloader) {
		return getClassesWithAnnotation(null, annotationClass, classloader);
	}
	
	public static Set<Class<?>> getClassesWithAnnotation(String packagePrefix, Class<? extends Annotation> annotationClass, ClassLoader classloader) {
		AnnotationScanner scanner = getAnnotationScannerInstance(packagePrefix, classloader);
		return scanner.getClassesWithAnnotation_(annotationClass, classloader);
	}
	
	private Set<Class<?>> getClassesWithAnnotation_(Class<? extends Annotation> annotationClass, ClassLoader classloader) {
		ClassInfoList classInfos = scanResult.getClassesWithAnnotation(annotationClass.getName());
		return loadClassesFromClassInfoList(classloader, classInfos);
	}
	
	public static Set<Method> getMethodsWithAnnotation(Class<? extends Annotation> annotationClass) {
		return getMethodsWithAnnotation(annotationClass, Thread.currentThread().getContextClassLoader());
	}
	
	public static Set<Method> getMethodsWithAnnotation(Class<? extends Annotation> annotationClass, ClassLoader classloader) {
		return getMethodsWithAnnotation((String)null, annotationClass, classloader);
	}
	
	
	public static Set<Method> getMethodsWithAnnotation(Class<? extends Annotation> annotationClass,	File file) {
		try {
			URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {file.toURI().toURL()});
			return getMethodsWithAnnotation(annotationClass, urlClassLoader);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Scan the URLs of the provided {@link URLClassLoader} and return the list of methods annotated
	 * by the provided Annotation. Important: This method isn't using cache.
	 *  
	 * @param annotationClass
	 * @param classloader
	 * @return
	 */
	public static Set<Method> getMethodsWithAnnotation(Class<? extends Annotation> annotationClass,	URLClassLoader classloader) {
		List<String> jars = Arrays.asList(classloader.getURLs()).stream()
				.map(url -> url.getPath()).collect(Collectors.toList());

		ClassGraph classGraph = new ClassGraph().overrideClasspath(jars)
				.enableClassInfo().enableAnnotationInfo().enableMethodInfo();
		try (ScanResult scanResult = classGraph.scan()) {
			Set<Method> methods = getMethodsWithAnnotation(scanResult, annotationClass, classloader);
			return methods;
		}
	}
	
	public static Set<Method> getMethodsWithAnnotation(String packagePrefix, Class<? extends Annotation> annotationClass, ClassLoader classloader) {
		AnnotationScanner scanner = getAnnotationScannerInstance(packagePrefix, classloader);
		return scanner.getMethodsWithAnnotation_(annotationClass, classloader);
	}

	protected static AnnotationScanner getAnnotationScannerInstance(String packagePrefix, ClassLoader classLoader) {
		String key = packagePrefix;
		if(packagePrefix == null) {
			key = ALLPACKAGES_PREFIXES;
		}
		AnnotationScanner scanner = scanResults.computeIfAbsent(new Key(key, classLoader), k->packagePrefix!=null?new AnnotationScanner(classLoader, packagePrefix):new AnnotationScanner(classLoader));
		return scanner;
	}
	
	private Set<Method> getMethodsWithAnnotation_(Class<? extends Annotation> annotationClass, ClassLoader classloader) {
		return getMethodsWithAnnotation(scanResult, annotationClass, classloader);
	}

	public static Set<Method> getMethodsWithAnnotation(ScanResult scanResult, Class<? extends Annotation> annotationClass,
			ClassLoader classloader) {
		Set<Method> result = new HashSet<>();
		ClassInfoList classInfos = scanResult.getClassesWithMethodAnnotation(annotationClass.getName());
		Set<Class<?>> classesFromClassInfoList = loadClassesFromClassInfoList(classloader, classInfos);
		classesFromClassInfoList.forEach(c->{
			Method[] methods = c.getMethods();
			for (Method method : methods) {
				if(isAnnotationPresent(annotationClass, method)) {
					result.add(method);
				}
			}
		});
		return result;
	}

	/**
	 * Alternative implementation of {@link Class#isAnnotationPresent(Class)} which doesn't rely
	 * on class equality but class names. The class loaders of the annotationClass and the 
	 * method provided as argument
	 * might be different
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
	public void close() throws IOException {
		scanResult.close();
	}
	
	public static void clearCache() {
		Collection<AnnotationScanner> values = new ArrayList<>(scanResults.values());
		scanResults.clear();
		values.forEach(v->{
			try {
				v.close();
			} catch (IOException e) {
				logger.error("Error while closing "+v.toString(), e);
			}
		});
	}
	
	private static final class Key {
		
		private final String prefix;
		private final ClassLoader classLoader;
		
		public Key(String prefix, ClassLoader classLoader) {
			super();
			this.prefix = prefix;
			this.classLoader = classLoader;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((classLoader == null) ? 0 : classLoader.hashCode());
			result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (classLoader == null) {
				if (other.classLoader != null)
					return false;
			} else if (!classLoader.equals(other.classLoader))
				return false;
			if (prefix == null) {
				if (other.prefix != null)
					return false;
			} else if (!prefix.equals(other.prefix))
				return false;
			return true;
		}
	}
}
