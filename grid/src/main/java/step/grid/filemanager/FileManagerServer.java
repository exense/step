package step.grid.filemanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import step.commons.helpers.FileHelper;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;

public class FileManagerServer implements FileProvider {

	ConcurrentHashMap<String, File> registry = new ConcurrentHashMap<>();
	
	ConcurrentHashMap<File, String> reverseRegistry = new ConcurrentHashMap<>();
	
	public String registerFile(File file) {
		String handle = reverseRegistry.computeIfAbsent(file, new Function<File, String>() {
			@Override
			public String apply(File t) {
				if(!file.exists()||!file.canRead()) {
					throw new RuntimeException("Unable to find or read file "+file.getAbsolutePath());
				}
				
				String handle = UUID.randomUUID().toString();
				registry.put(handle, file);
				return handle;
			}
		});
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
