package step.core.objectenricher;

import java.util.Map;
import java.util.function.Consumer;

import step.core.accessors.AbstractOrganizableObject;

/**
 * Instances of this class are responsible for the enrichment of 
 * entities with context parameters. Enrichment refers to the process of
 * adding context parameters to the entites that are subject to it 
 * (like {@link AbstractOrganizableObject} for instance) 
 */
public interface ObjectEnricher extends Consumer<Object> {
	
	public Map<String, String> getAdditionalAttributes();
}