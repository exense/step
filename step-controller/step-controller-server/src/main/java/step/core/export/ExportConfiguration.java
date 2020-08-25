package step.core.export;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;

public class ExportConfiguration {
	
	OutputStream outputStream;
	ObjectEnricher objectEnricher;
	Map<String, String> metadata;
	ObjectPredicate objectPredicate;
	String entityType;
	boolean recursively;
	List<String> additionalEntities;
	
	public ExportConfiguration(OutputStream outputStream, ObjectEnricher objectEnricher, Map<String, String> metadata,
			ObjectPredicate objectPredicate, String entityType, boolean recursively, List<String> additionalEntities) {
		super();
		this.outputStream = outputStream;
		this.objectEnricher = objectEnricher;
		this.metadata = metadata;
		this.objectPredicate = objectPredicate;
		this.entityType = entityType;
		this.recursively = recursively;
		this.additionalEntities = additionalEntities;
	}
	

	public OutputStream getOutputStream() {
		return outputStream;
	}
	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}
	public ObjectEnricher getObjectEnricher() {
		return objectEnricher;
	}
	public void setObjectEnricher(ObjectEnricher objectEnricher) {
		this.objectEnricher = objectEnricher;
	}
	public Map<String, String> getMetadata() {
		return metadata;
	}
	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}
	public ObjectPredicate getObjectPredicate() {
		return objectPredicate;
	}
	public void setObjectPredicate(ObjectPredicate objectPredicate) {
		this.objectPredicate = objectPredicate;
	}
	public String getEntityType() {
		return entityType;
	}
	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}
	public boolean isRecursively() {
		return recursively;
	}
	public void setRecursively(boolean recursively) {
		this.recursively = recursively;
	}
	public List<String> getAdditionalEntities() {
		return additionalEntities;
	}
	public void setAdditionalEntities(List<String> additionalEntities) {
		this.additionalEntities = additionalEntities;
	}
}
