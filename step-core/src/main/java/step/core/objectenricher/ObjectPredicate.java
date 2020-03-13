package step.core.objectenricher;

import java.util.function.Predicate;

/**
 * Classes implementing this interface implement the context filtering of POJO objects
 * The predicate implements the filter specified in {@link ObjectFilter} and is responsible
 * for the filter of objects according to the current context filters
 *
 */
public interface ObjectPredicate extends Predicate<Object> {

}
