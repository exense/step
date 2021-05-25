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
package step.core.export;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import step.core.objectenricher.ObjectPredicate;

public class ExportConfiguration {

	private final OutputStream outputStream;
	private final Map<String, String> metadata;
	private final ObjectPredicate objectPredicate;
	private final String entityType;
	private final boolean recursively;
	private final List<String> additionalEntities;

	public ExportConfiguration(OutputStream outputStream, Map<String, String> metadata, ObjectPredicate objectPredicate,
			String entityType, boolean recursively, List<String> additionalEntities) {
		super();
		this.outputStream = outputStream;
		this.metadata = metadata;
		this.objectPredicate = objectPredicate;
		this.entityType = entityType;
		this.recursively = recursively;
		this.additionalEntities = additionalEntities;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public ObjectPredicate getObjectPredicate() {
		return objectPredicate;
	}

	public String getEntityType() {
		return entityType;
	}

	public boolean isRecursively() {
		return recursively;
	}

	public List<String> getAdditionalEntities() {
		return additionalEntities;
	}
}
