package step.core.scanner;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
	
	private static final ConcurrentHashMap<String, AnnotationScanner> scanResults = new ConcurrentHashMap<>();
	
	private AnnotationScanner(String... packagePrefixes) {
		super();
		long t1 = System.currentTimeMillis();
		logger.info("Scanning classpath...");
		ClassGraph classGraph = new ClassGraph().whitelistPackages(packagePrefixes).enableClassInfo().enableAnnotationInfo().enableMethodInfo();
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
		AnnotationScanner scanner = getAnnotationScannerInstance(packagePrefix);
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
		return getMethodsWithAnnotation(null, annotationClass, classloader);
	}
	
	public static Set<Method> getMethodsWithAnnotation(String packagePrefix, Class<? extends Annotation> annotationClass, ClassLoader classloader) {
		AnnotationScanner scanner = getAnnotationScannerInstance(packagePrefix);
		return scanner.getMethodsWithAnnotation_(annotationClass, classloader);
	}

	protected static AnnotationScanner getAnnotationScannerInstance(String packagePrefix) {
		String key = packagePrefix;
		if(packagePrefix == null) {
			key = ALLPACKAGES_PREFIXES;
		}
		AnnotationScanner scanner = scanResults.computeIfAbsent(key, k->packagePrefix!=null?new AnnotationScanner(packagePrefix):new AnnotationScanner());
		return scanner;
	}
	
	private Set<Method> getMethodsWithAnnotation_(Class<? extends Annotation> annotationClass, ClassLoader classloader) {
		Set<Method> result = new HashSet<>();
		ClassInfoList classInfos = scanResult.getClassesWithMethodAnnotation(annotationClass.getName());
		Set<Class<?>> classesFromClassInfoList = loadClassesFromClassInfoList(classloader, classInfos);
		classesFromClassInfoList.forEach(c->{
			Method[] methods = c.getMethods();
			for (Method method : methods) {
				if(method.isAnnotationPresent(annotationClass)) {
					result.add(method);
				}
			}
		});
		return result;
	}

	private Set<Class<?>> loadClassesFromClassInfoList(ClassLoader classloader, ClassInfoList classInfos) {
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
}
