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
package step.core.imports;

import java.io.IOException;
import java.util.Map;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.exense.commons.core.model.accessors.AbstractOrganizableObject;
import ch.exense.commons.core.collections.Collection;
import ch.exense.commons.core.collections.Document;

public class GenericImporter implements Importer {

	protected final ImportConfiguration importConfig;
	protected final Collection<Document> tmpCollection;
	protected final Map<String, String> references;
	protected final Map<String, String> newToOldReferences;
	protected final ObjectMapper objectMapper;

	public GenericImporter(ImportConfiguration importConfig, Collection<Document> tmpCollection,
			ObjectMapper objectMapper) {
		super();
		this.importConfig = importConfig;
		this.tmpCollection = tmpCollection;
		this.references = importConfig.getReferences();
		this.newToOldReferences = importConfig.getNewToOldReferences();
		this.objectMapper = objectMapper;
	}

	public void importOne(JsonParser jParser) throws JsonParseException, JsonMappingException, IOException {
		Document o = objectMapper.readValue(jParser, Document.class);
		if (o.containsKey("_id")) {
			o.put(AbstractOrganizableObject.ID, o.get("_id"));
			o.remove("_id");
		}
		if (importConfig.isOverwrite()) {
			tmpCollection.save(o);
		} else {
			saveWithNewId(o);
		}
	}

	protected void saveWithNewId(Document aObj) {
		String origId = aObj.getId().toHexString();
		ObjectId objectId;
		// if the origId was already replaced, use the new one
		if (references.containsKey(origId)) {
			objectId = new ObjectId(references.get(origId));
		} else {
			objectId = new ObjectId();
		}
		aObj.setId(objectId);
		references.put(origId, aObj.getId().toHexString());
		newToOldReferences.put(aObj.getId().toHexString(), origId);

		tmpCollection.save(aObj);
	}
}
