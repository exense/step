package step.grid.filemanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
	
	public Attachment getFile(String fileHandle) {
		File file = registry.get(fileHandle);;
		byte[] bytes;
		try {
			bytes = Files.readAllBytes(file.toPath());
			Attachment attachment = AttachmentHelper.generateAttachmentFromByteArray(bytes, file.getName());
			return attachment;
		} catch (IOException e) {
			throw new RuntimeException("Error while reading file with handle "+fileHandle+" mapped to '"+file.getAbsolutePath()+"'", e);
		}
	}
	
}
