package step.functions.type;

public class FunctionTypeConfiguration {
	
	private int fileResolverCacheConcurrencyLevel = 4;
	
	private int fileResolverCacheMaximumsize = 1000;
	
	private int fileResolverCacheExpireAfter = 500;

	public int getFileResolverCacheConcurrencyLevel() {
		return fileResolverCacheConcurrencyLevel;
	}

	public void setFileResolverCacheConcurrencyLevel(int fileResolverCacheConcurrencyLevel) {
		this.fileResolverCacheConcurrencyLevel = fileResolverCacheConcurrencyLevel;
	}

	public int getFileResolverCacheMaximumsize() {
		return fileResolverCacheMaximumsize;
	}

	public void setFileResolverCacheMaximumsize(int fileResolverCacheMaximumsize) {
		this.fileResolverCacheMaximumsize = fileResolverCacheMaximumsize;
	}

	public int getFileResolverCacheExpireAfter() {
		return fileResolverCacheExpireAfter;
	}

	public void setFileResolverCacheExpireAfter(int fileResolverCacheExpireAfter) {
		this.fileResolverCacheExpireAfter = fileResolverCacheExpireAfter;
	}
}
