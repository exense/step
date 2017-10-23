package step.grid.filemanager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.commons.helpers.FileHelper;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;

public class FileManagerClientImpl implements FileManagerClient {
	
	private static final Logger logger = LoggerFactory.getLogger(FileManagerClientImpl.class);
	
	private File dataFolder;
	
	private FileProvider fileProvider;

	private Map<String, FileInfo> cache = new ConcurrentHashMap<>();

	public FileManagerClientImpl(File dataFolder, FileProvider fileProvider) {
		super();
		logger.info("Starting file manager client using data folder: "+dataFolder.getAbsolutePath());
		this.dataFolder = dataFolder;
		this.fileProvider = fileProvider;
	}

	static class FileInfo {
		
		final String uid;
		
		File file;
		
		long lastModified;

		public FileInfo(String uid) {
			super();
			this.uid = uid;
		}
	}	
	
	public File requestFile(String uid, long lastModified) {
		return requestFileVersion(uid, lastModified).getFile();
	}
	
	@Override
	public FileVersion requestFileVersion(String uid, long lastModified) {
		FileVersion response = new FileVersion();
		response.setFileId(uid);
		
		if(logger.isDebugEnabled()) {
			logger.debug("Got file request for file id: "+uid+" and version "+Long.toString(lastModified));
		}
		
		FileInfo fileInfo = cache.get(uid);
		
		if(fileInfo==null) {
			fileInfo = new FileInfo(uid);
			
			FileInfo currentValue = cache.putIfAbsent(uid, fileInfo);
			if(currentValue!=null) {
				fileInfo = currentValue;
			}
		}
		
		boolean fileModication;
		
		synchronized (fileInfo) {
			if(fileInfo.file==null) {
				if(logger.isDebugEnabled()) {
					logger.debug("Cache miss for file id: "+uid+" and version "+Long.toString(lastModified)+". Requesting file from server");
				}
				requestFileAndUpdateCache(fileInfo, uid, lastModified);
				fileModication = true;
			} else if(lastModified>fileInfo.lastModified) {
				if(logger.isDebugEnabled()) {
					logger.debug("File version mismatch for file id: "+uid+" and version "+Long.toString(lastModified)+". Requesting file from server");
				}
				requestFileAndUpdateCache(fileInfo, uid, lastModified);
				fileModication = true;
			} else {
				// local file is up to date
				if(logger.isDebugEnabled()) {
					logger.debug("Served file request from cache. file id: "+uid+" and version "+Long.toString(lastModified)+". Requesting file from server");
				}
				fileModication = false;
			}			
		}
		
		response.setModified(fileModication);
		response.setFile(fileInfo.file);
		return response;
	}

	private void requestFileAndUpdateCache(FileInfo fileInfo, String uid, long lastModified) {
		File file = requestControllerFile(uid);
		updateFileInfo(fileInfo, file, lastModified);
	}

	private void updateFileInfo(FileInfo fileInfo, File file, long lastModified) {
		fileInfo.file = file;
		fileInfo.lastModified = lastModified;
	}
	
	private File requestControllerFile(String fileId) {
		long t1 = System.currentTimeMillis();
		Attachment attachment = fileProvider.getFileAsAttachment(fileId);
		if(logger.isDebugEnabled()) {
			logger.debug("Got file "+ fileId +" from server in "+(System.currentTimeMillis()-t1)+"ms.");
		}
		
		long t2 = System.currentTimeMillis();
		byte[] bytes = AttachmentHelper.hexStringToByteArray(attachment.getHexContent());
		
		File container = new File(dataFolder+"/"+fileId);
		if(container.exists()) {
			container.delete();
		}
		container.mkdirs();
		container.deleteOnExit();		
		
		File file = new File(container+"/"+attachment.getName());
		if(attachment.getIsDirectory()) {
			FileHelper.extractFolder(bytes, file);
		} else {
			try {
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
				bos.write(bytes);
				bos.close();
			} catch (IOException ex) {
				
			}						
		}
		if(logger.isDebugEnabled()) {
			logger.debug("Uncompressed file "+ fileId +" in "+(System.currentTimeMillis()-t2)+"ms to "+file.getAbsoluteFile());
		}
		return file.getAbsoluteFile();	
	}
}
