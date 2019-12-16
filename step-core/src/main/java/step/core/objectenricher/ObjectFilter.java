package step.core.objectenricher;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Instances of this classes are responsible for the context filtering of objects
 *
 */
public interface ObjectFilter extends Predicate<Object> {

	/**
	 * @return the list of attributes required by the current context when filtering a collection of objects
	 */
	public Map<String, String> getAdditionalAttributes();
}
