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

import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.FileResolver;
import step.attachments.FileResolver.FileHandle;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.objectenricher.ObjectHookRegistry;

@Singleton
@Path("import")
public class ImportServices extends AbstractServices {
	
	private static final Logger logger = LoggerFactory.getLogger(ImportServices.class);
	
	FileResolver fileResolver;
	
	ImportManager importManager;
	ObjectHookRegistry objectHookRegistry;
		
	@PostConstruct
	public void init() throws Exception {
		super.init();
		importManager = new ImportManager(getContext());
		fileResolver = getContext().getFileResolver();
		objectHookRegistry = getContext().get(ObjectHookRegistry.class);
	}

	@POST
	@Path("/{entity}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public void importEntity(@PathParam("entity") String entity, @QueryParam("path") String path, @QueryParam("importAll") boolean importAll, @QueryParam("overwrite") boolean overwrite) throws Exception {
		try (FileHandle file = fileResolver.resolveFileHandle(path)) {
			List<String> filter = null;
			if (!importAll) {
				filter = Arrays.asList(entity);
			}
			ImportConfiguration importConfiguration = new ImportConfiguration(file.getFile(), getObjectEnricher(),getObjectDrainer(),filter,overwrite);
			importManager.importAll(importConfiguration);
		}
	}
}
