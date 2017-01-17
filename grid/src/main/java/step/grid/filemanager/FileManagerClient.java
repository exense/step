package step.grid.filemanager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;

public class FileManagerClient {
	
	File dataFolder;
	
	FileProvider fileProvider;

	public FileManagerClient(File dataFolder, FileProvider fileProvider) {
		super();
		this.dataFolder = dataFolder;
		this.fileProvider = fileProvider;
	}

	static class FileInfo {
		
		String uid;
		
		File file;
		
		long lastModified;

		public FileInfo(String uid, File file, long lastModified) {
			super();
			this.uid = uid;
			this.file = file;
			this.lastModified = lastModified;
		}
	}
	
	private Map<String, FileInfo> cache = new ConcurrentHashMap<>();
	
	public File requestFile(String uid, long lastModified) {
		FileInfo fileInfo = cache.get(uid);
		if(fileInfo==null) {
			fileInfo = requestFileAndUpdateCache(uid, lastModified);
		} else {
			if(lastModified>fileInfo.lastModified) {
				fileInfo = requestFileAndUpdateCache(uid, lastModified);
			} else {
				// local file is up to date
			}
		}
		return fileInfo.file;
	}

	private FileInfo requestFileAndUpdateCache(String uid, long lastModified) {
		File file = requestControllerFile(uid);
		return updateCache(uid, file, lastModified);
	}

	private FileInfo updateCache(String uid, File file, long lastModified) {
		FileInfo fileInfo;
		fileInfo = new FileInfo(uid, file, lastModified);
		cache.put(uid, fileInfo);
		return fileInfo;
	}
	
	private File requestControllerFile(String fileId) {
		Attachment attachment = fileProvider.getFile(fileId);
		
		File file = new File(dataFolder+"/"+attachment.getName());
		file.deleteOnExit();
		
		try {
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			bos.write(AttachmentHelper.hexStringToByteArray(attachment.getHexContent()));
			bos.close();
		} catch (IOException ex) {

		}
		return file;	
	}
}
