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

import ch.exense.commons.io.FileHelper;
import step.core.Version;
import step.core.collections.CollectionFactory;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.resources.LocalResourceManagerImpl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ImportContext implements AutoCloseable {

	private final ImportConfiguration importConfiguration;

	private Version version;
	private Map<String, String> metadata;

	private final File workFolder;
	private final LocalResourceManagerImpl localResourceMgr;

	private final CollectionFactory tempCollectionFactory;

	private final Map<String, String> references = new HashMap<String, String>();
	private final Map<String, String> newToOldReferences = new HashMap<String, String>();
	private final Set<String> messages = new HashSet<>();

	public ImportContext(ImportConfiguration importConfiguration) throws IOException {
		super();
		this.importConfiguration = importConfiguration;

		tempCollectionFactory = new InMemoryCollectionFactory(null);
		workFolder = FileHelper.createTempFolder("step-import");
		localResourceMgr = new LocalResourceManagerImpl(workFolder);
	}

	public ImportConfiguration getImportConfiguration() {
		return importConfiguration;
	}

	public Version getVersion() {
		return version;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	public LocalResourceManagerImpl getLocalResourceMgr() {
		return localResourceMgr;
	}

	public CollectionFactory getTempCollectionFactory() {
		return tempCollectionFactory;
	}

	public Map<String, String> getReferences() {
		return references;
	}

	public Map<String, String> getNewToOldReferences() {
		return newToOldReferences;
	}

	public Set<String> getMessages() {
		return messages;
	}

	public boolean addMessage(String e) {
		return messages.add(e);
	}

	public File getWorkFolder() {
		return workFolder;
	}

	@Override
	public void close() throws Exception {
		FileHelper.deleteFolder(workFolder);
	}
}
