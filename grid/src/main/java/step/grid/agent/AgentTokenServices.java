package step.grid.agent;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;

public class AgentTokenServices {
	
	RegistrationClient gridClient;
	
	File dataFolder;
	
	public AgentTokenServices(RegistrationClient gridClient, File dataFolder) {
		super();
		this.gridClient = gridClient;
		this.dataFolder = dataFolder;
	}

	public File requestControllerFile(String fileId) {
		Attachment attachment = gridClient.requestFile(fileId);
		
		File file = new File(dataFolder+"/"+attachment.getName());
		
		try {
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			bos.write(AttachmentHelper.hexStringToByteArray(attachment.getHexContent()));
			bos.close();
		} catch (IOException ex) {

		}
		return file;	
	}

}
