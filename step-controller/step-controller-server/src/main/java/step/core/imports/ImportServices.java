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
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.tags.Tag;
import step.attachments.FileResolver;
import step.attachments.FileResolver.FileHandle;
import step.core.GlobalContext;
import step.core.Version;
import step.core.deployment.AbstractStepServices;
import step.framework.server.security.Secured;
import step.migration.MigrationManager;

@Singleton
@Path("import")
@Tag(name = "Imports")
public class ImportServices extends AbstractStepServices {
	
	private FileResolver fileResolver;
	private ImportManager importManager;
		
	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		importManager = new ImportManager(context.getEntityManager(), context.require(MigrationManager.class),
				context.getResourceManager(), context.require(Version.class));
		fileResolver = context.getFileResolver();
	}

	@POST
	@Path("/{entity}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public Set<String> importEntity(@PathParam("entity") String entity, @QueryParam("path") String path, @QueryParam("importAll") boolean importAll, @QueryParam("overwrite") boolean overwrite) throws Exception {
		try (FileHandle file = fileResolver.resolveFileHandle(path)) {
			List<String> filter = null;
			if (!importAll) {
				filter = Arrays.asList(entity);
			}
			ImportConfiguration importConfiguration = new ImportConfiguration(file.getFile(), getObjectEnricher(), filter, overwrite);
			ImportResult importResult = importManager.importAll(importConfiguration);
			return importResult.getMessages();
		}
	}
}
