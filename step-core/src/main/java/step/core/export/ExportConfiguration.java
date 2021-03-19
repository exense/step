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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;

public class ExportConfiguration {
	
	private OutputStream outputStream;
	private Map<String, String> metadata;
	private ObjectPredicate objectPredicate;
	private String entityType;
	private boolean recursively;
	private List<String> additionalEntities;
	private Set<String> messages = new HashSet<>();
	
	public ExportConfiguration(OutputStream outputStream, Map<String, String> metadata,
							   ObjectPredicate objectPredicate, String entityType, boolean recursively, List<String> additionalEntities) {
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
	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}
	public Map<String, String> getMetadata() {
		return metadata;
	}
	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}
	public ObjectPredicate getObjectPredicate() {
		return objectPredicate;
	}
	public void setObjectPredicate(ObjectPredicate objectPredicate) {
		this.objectPredicate = objectPredicate;
	}
	public String getEntityType() {
		return entityType;
	}
	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}
	public boolean isRecursively() {
		return recursively;
	}
	public void setRecursively(boolean recursively) {
		this.recursively = recursively;
	}
	public List<String> getAdditionalEntities() {
		return additionalEntities;
	}
	public void setAdditionalEntities(List<String> additionalEntities) {
		this.additionalEntities = additionalEntities;
	}

	public void getContext() {
	}

	public void addMessages(Set<String> newMessages) {
		messages.addAll(newMessages);
	}

	public Set<String> getMessages() {
		return messages;
	}

	public void addMessage(String s) {
		messages.add(s);
	}
}
