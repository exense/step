package step.core.imports;

import java.io.File;
import java.util.List;
import java.util.Map;

import step.core.Version;
import step.core.objectenricher.ObjectEnricher;
import step.resources.LocalResourceManagerImpl;

public class ImportConfiguration {
	File file;
	ObjectEnricher objectEnricher;
	List<String> entitiesFilter;
	boolean overwrite;
	Map<String,String> metadata;
	Version version;
	LocalResourceManagerImpl localResourceMgr;
	
	public ImportConfiguration(File file, ObjectEnricher objectEnricher, List<String> entitiesFilter,
			boolean overwrite) {
		super();
		this.file = file;
		this.objectEnricher = objectEnricher;
		this.entitiesFilter = entitiesFilter;
		this.overwrite = overwrite;
	}
	public File getFile() {
		return file;
	}
	public void setFile(File file) {
		this.file = file;
	}
	public ObjectEnricher getObjectEnricher() {
		return objectEnricher;
	}
	public void setObjectEnricher(ObjectEnricher objectEnricher) {
		this.objectEnricher = objectEnricher;
	}
	public List<String> getEntitiesFilter() {
		return entitiesFilter;
	}
	public void setEntitiesFilter(List<String> entitiesFilter) {
		this.entitiesFilter = entitiesFilter;
	}
	public boolean isOverwrite() {
		return overwrite;
	}
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}
	public Map<String, String> getMetadata() {
		return metadata;
	}
	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}
	public Version getVersion() {
		return version;
	}
	public void setVersion(Version version) {
		this.version = version;
	}
	public LocalResourceManagerImpl getLocalResourceMgr() {
		return localResourceMgr;
	}
	public void setLocalResourceMgr(LocalResourceManagerImpl localResourceMgr) {
		this.localResourceMgr = localResourceMgr;
	}
}
