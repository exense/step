package step.grid.bootstrap;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileManagerClient.FileVersion;
import step.grid.filemanager.FileManagerClient.FileVersionId;
import step.grid.isolation.ClassPathHelper;

public class RemoteClassPathBuilder {
	
	protected FileManagerClient fileManager;
	
	public RemoteClassPathBuilder(FileManagerClient fileManager) {
		super();
		this.fileManager = fileManager;
	}

	public static class RemoteClassPath {
		
		List<URL> urls;
		
		boolean forceReload;
		
		String key;

		public List<URL> getUrls() {
			return urls;
		}

		public void setUrls(List<URL> urls) {
			this.urls = urls;
		}

		public boolean addUrl(URL e) {
			return urls.add(e);
		}

		public boolean isForceReload() {
			return forceReload;
		}

		public void setForceReload(boolean forceReload) {
			this.forceReload = forceReload;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}
	}

	public RemoteClassPath buildRemoteClassPath(FileVersionId handlerPackage) throws IOException, Exception {
		String contextKey;
		List<URL> urls;
		boolean forceReload;
		if(handlerPackage!=null) {
			FileVersion libFolder = fileManager.requestFileVersion(handlerPackage.getFileId(), handlerPackage.getVersion());
			contextKey = libFolder.getFileId();
			
			forceReload = libFolder.isModified();
			if (libFolder.getFile().isDirectory()) {
				urls = ClassPathHelper.forAllJarsInFolder(libFolder.getFile());
			} else {
				urls = ClassPathHelper.forSingleFile(libFolder.getFile());
			}	
		} else {
			contextKey = "default";
			urls = new ArrayList<>();
			forceReload = false;
		}
		
		RemoteClassPath result = new RemoteClassPath();
		result.forceReload = forceReload;
		result.key = contextKey;
		result.urls = urls;
		return result;
	}
}
