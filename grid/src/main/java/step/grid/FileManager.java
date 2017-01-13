package step.grid;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;

public class FileManager {

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
	
	public File getFile(String fileHandle) {
		return registry.get(fileHandle);
	}
	
	public Attachment getFileAsAttachment(String fileHandle) throws IOException {
		File file = getFile(fileHandle);
		byte[] bytes = Files.readAllBytes(file.toPath());
		Attachment attachment = AttachmentHelper.generateAttachmentFromByteArray(bytes, file.getName());
		return attachment;
	}
	
}
