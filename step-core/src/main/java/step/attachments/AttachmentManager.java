/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.attachments;

import java.io.File;

import org.bson.types.ObjectId;

import step.commons.conf.Configuration;
import step.commons.helpers.FileHelper;

public class AttachmentManager {
	
	protected Configuration configuration;
	
	protected String filerPath;
	
	public AttachmentManager(Configuration configuration) {
		super();
		this.configuration = configuration;
		this.filerPath = configuration.getProperty("attachmentsdir","attachments");
	}
	
	public AttachmentContainer createAttachmentContainer() {
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
	
	private File getContainerFileById(ObjectId id) {
		return new File(filerPath + "/" + id.toString());
	}
	
	public File getFileById(String id) {
		return getFileById(new ObjectId(id));
	}
	
	@Deprecated
	public File getFileById(ObjectId id) {
		File folder = getContainerFileById(id);
		String[] list = folder.list();
		if(list!=null && list.length>0) {
			File file = new File(folder+"/"+list[0]);
			return file;
		} else {
			return null;
		}	
	}
	
	public void deleteContainer(String id) {
		File folder = getContainerFileById(new ObjectId(id));
		FileHelper.deleteFolder(folder);
	}
}
