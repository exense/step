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

import java.io.File;
import java.util.List;

import step.core.objectenricher.ObjectEnricher;

public class ImportConfiguration {

	private final File file;
	private final ObjectEnricher objectEnricher;
	private final List<String> entitiesFilter;
	private final boolean overwrite;

	public ImportConfiguration(File file, ObjectEnricher objectEnricher, List<String> entitiesFilter,
							   boolean overwrite) {
		super();
		this.file = file;
		this.objectEnricher = objectEnricher;
		this.entitiesFilter = entitiesFilter;
		this.overwrite = overwrite;
	}

	public File getFile() {
		return file;
	}

	public ObjectEnricher getObjectEnricher() {
		return objectEnricher;
	}

	public List<String> getEntitiesFilter() {
		return entitiesFilter;
	}

	public boolean isOverwrite() {
		return overwrite;
	}
}
