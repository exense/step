package step.core.functions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.scanner.CachedAnnotationScanner;
import step.core.yaml.YamlModel;
import step.handlers.javahandler.Keyword;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class StandardKeywordsLookuper {

    private static final Logger log = LoggerFactory.getLogger(StandardKeywordsLookuper.class);

    public List<String> lookupStandardKeywords() {
        log.info("Trying to lookup standard keywords included in distribution...");
        List<String> standardKeywords = new ArrayList<>();
        Set<Class<?>> standardKeywordSourceClasses = CachedAnnotationScanner.getClassesWithAnnotation(YamlModel.LOCATION, StandardKeywordSource.class, Thread.currentThread().getContextClassLoader());
        for (Class<?> standardKeywordSourceClass : standardKeywordSourceClasses) {
            Class<?> klass = standardKeywordSourceClass;
            while (klass != Object.class) { // need to traverse a type hierarchy in order to process methods from super types
                // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
                for (final Method method : klass.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Keyword.class)) {
                        Keyword annotInstance = method.getAnnotation(Keyword.class);
                        if (annotInstance.name() != null) {
                            standardKeywords.add(annotInstance.name());
                        }
                    }
                }
                // move to the upper class in the hierarchy in search for more methods
                klass = klass.getSuperclass();
            }
        }
        standardKeywords.sort(Comparator.naturalOrder());
        log.info("The following standard keyword have been found: {}", standardKeywords);
        return standardKeywords;
    }

}
