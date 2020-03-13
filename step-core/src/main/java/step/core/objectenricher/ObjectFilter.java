package step.core.objectenricher;

import step.core.ql.OQLParser;

/**
 * Instances of this classes are responsible for the context filtering of objects
 *
 */
public interface ObjectFilter {

	/**
	 * @return the OQL Query fragment specifying this filter. See {@link OQLParser} 
	 */
	public String getOQLFilter();
}
