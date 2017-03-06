package step.grid.filemanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import step.commons.helpers.FileHelper;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;

public class FileManagerServer implements FileProvider {

	Map<String, File> registry = new ConcurrentHashMap<>();
	
	Map<File, String> reverseRegistry = new HashMap<>();
	
	public synchronized String registerFile(File file) {
		String handle = reverseRegistry.get(file);
		if(handle==null) {
			String newHandle = UUID.randomUUID().toString();
			registry.put(newHandle, file);
			reverseRegistry.put(file, newHandle);
			handle = newHandle;
		}
		return handle;
	}
	
	public Attachment getFileAsAttachment(String fileHandle) {
		File file = getFile(fileHandle);
		return generateAttachment(fileHandle, file);
	}

	protected Attachment generateAttachment(String fileHandle, File transferFile) {
		byte[] bytes;
		boolean isDirectory;
		try {
			if(transferFile.isDirectory()) {
				bytes = FileHelper.zipDirectory(transferFile);
				isDirectory = true;
			} else {
				bytes = Files.readAllBytes(transferFile.toPath());	
				isDirectory = false;
			}
			Attachment attachment = AttachmentHelper.generateAttachmentFromByteArray(bytes, transferFile.getName());
			attachment.setIsDirectory(isDirectory);
			
			return attachment;
		} catch (IOException e) {
			throw new RuntimeException("Error while reading file with handle "+fileHandle+" mapped to '"+transferFile.getAbsolutePath()+"'", e);
		}
	}

	public File getFile(String fileHandle) {
		return registry.get(fileHandle);
	}
	
}
