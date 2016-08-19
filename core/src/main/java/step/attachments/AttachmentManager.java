package step.attachments;

import java.io.File;

import org.bson.types.ObjectId;

import step.commons.conf.Configuration;

public class AttachmentManager {
	
	private static String filerPath = Configuration.getInstance().getProperty("attachmentsdir","attachments");
	
	public static AttachmentContainer createAttachmentContainer() {
		AttachmentMeta meta = new AttachmentMeta();
		
		AttachmentMetaAccessor accessor = new AttachmentMetaAccessor();
		accessor.save(meta);
		
		File containerFolder = getContainerFileById(meta.getId());
		containerFolder.mkdirs();
		
		AttachmentContainer container = new AttachmentContainer();
		container.setMeta(meta);		
		container.setContainer(containerFolder);
		
		return container;
	}
	
	private static File getContainerFileById(ObjectId id) {
		return new File(filerPath + "/" + id.toString());
	}
	
	public static File getFileById(ObjectId id) {
		File folder = getContainerFileById(id);
		String[] list = folder.list();
		if(list!=null && list.length>0) {
			File file = new File(folder+"/"+list[0]);
			return file;
		} else {
			return null;
		}	
	}
}
