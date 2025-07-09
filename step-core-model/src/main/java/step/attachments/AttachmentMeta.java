/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.attachments;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bson.types.ObjectId;

@JsonTypeInfo(
		use= JsonTypeInfo.Id.CLASS,
		property = "type",
		defaultImpl = AttachmentMeta.class  // Fallback type if "type" is missing
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = SkippedAttachmentMeta.class),
		@JsonSubTypes.Type(value = StreamingAttachmentMeta.class),
})
public class AttachmentMeta {
	
	ObjectId _id;
	
	String name;

	public AttachmentMeta(ObjectId id) {
		super();
		_id = id;
	}
	
	public AttachmentMeta() {
		super();
		_id = new ObjectId();
	}
	
	public ObjectId getId() {
		return _id;
	}
	
	public void setId(ObjectId id) {
		this._id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
