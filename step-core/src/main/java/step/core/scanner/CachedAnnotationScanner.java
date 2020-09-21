package step.core.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This classes provides a list of static methods delegating annotation scanning
 * to {@link AnnotationScanner} and <b>caching</b> its results. A cache entry
 * for each combination of packagePrefix and {@link ClassLoader} will be
 * created. Calling this class for non-bounded sets of combinations will cause
 * memory issues. Please be therefore aware of what you're doing when using this
 * class
 * 
 *
 */
public class CachedAnnotationScanner {

	private static final ConcurrentHashMap<Key, AnnotationScanner> annotationScanners = new ConcurrentHashMap<>();

	private static final String ALLPACKAGES_PREFIXES = "";

	/**
	 * @param annotationClass
	 * @return a set of classes annotated by the provided annotation in the current
	 *         context class loader. <b>Warning:</b> Results of this methods are
	 *         cached. See Class comments for more details
	 */
	public static Set<Class<?>> getClassesWithAnnotation(Class<? extends Annotation> annotationClass) {
		return getClassesWithAnnotation(annotationClass, Thread.currentThread().getContextClassLoader());
	}

	/**
	 * @param annotationClass
	 * @param classloader
	 * @return a set of classes annotated by the provided annotation in the provided
	 *         class loader. <b>Warning:</b> Results of this methods are cached. See
	 *         Class comments for more details
	 */
	public static Set<Class<?>> getClassesWithAnnotation(Class<? extends Annotation> annotationClass,
			ClassLoader classloader) {
		return getClassesWithAnnotation(null, annotationClass, classloader);
	}

	/**
	 * @param packagePrefix
	 * @param annotationClass
	 * @param classloader
	 * @return a set of classes annotated by the provided annotation in the provided
	 *         class loader and within the provided package. <b>Warning:</b> Results
	 *         of this methods are cached. See Class comments for more details
	 */
	public static Set<Class<?>> getClassesWithAnnotation(String packagePrefix,
			Class<? extends Annotation> annotationClass, ClassLoader classloader) {
		AnnotationScanner scanner = getAnnotationScannerInstance(packagePrefix, classloader);
		return scanner.getClassesWithAnnotation(annotationClass);
	}

	/**
	 * @param annotationClass
	 * @return a set of methods annotated by the provided annotation in the current
	 *         context class loader. <b>Warning:</b> Results of this methods are
	 *         cached. See Class comments for more details
	 */
	public static Set<Method> getMethodsWithAnnotation(Class<? extends Annotation> annotationClass) {
		return getMethodsWithAnnotation(annotationClass, Thread.currentThread().getContextClassLoader());
	}

	/**
	 * @param annotationClass
	 * @param classloader
	 * @return a set of methods annotated by the provided annotation in the provided
	 *         class loader. <b>Warning:</b> Results of this methods are cached. See
	 *         Class comments for more details
	 */
	public static Set<Method> getMethodsWithAnnotation(Class<? extends Annotation> annotationClass,
			ClassLoader classloader) {
		return getMethodsWithAnnotation((String) null, annotationClass, classloader);
	}

	/**
	 * @param packagePrefix
	 * @param annotationClass
	 * @param classloader
	 * @return a set of classes annotated by the provided annotation in the provided
	 *         class loader and within the provided package. <b>Warning:</b> Results
	 *         of this methods are cached. See Class comments for more details
	 */
	public static Set<Method> getMethodsWithAnnotation(String packagePrefix,
			Class<? extends Annotation> annotationClass, ClassLoader classloader) {
		AnnotationScanner scanner = getAnnotationScannerInstance(packagePrefix, classloader);
		return scanner.getMethodsWithAnnotation(annotationClass);
	}

	private static AnnotationScanner getAnnotationScannerInstance(String packagePrefix, ClassLoader classLoader) {
		String key = packagePrefix;
		if (packagePrefix == null) {
			key = ALLPACKAGES_PREFIXES;
		}
		AnnotationScanner scanner = annotationScanners.computeIfAbsent(new Key(key, classLoader), k -> {
			return packagePrefix != null ? AnnotationScanner.forAllClassesFromClassLoader(packagePrefix, classLoader)
					: AnnotationScanner.forAllClassesFromClassLoader(classLoader);
		});
		return scanner;
	}

	public static void clearCache() {
		Collection<AnnotationScanner> values = new ArrayList<>(annotationScanners.values());
		annotationScanners.clear();
		values.forEach(v -> {
			v.close();
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
