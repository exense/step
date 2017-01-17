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
package step.grid;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import step.grid.filemanager.FileProvider;
import step.grid.io.Attachment;

@Path("/grid")
public class GridServices {

	@Inject
	Grid grid;
	
	@Inject
	FileProvider fileManager;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/register")
	public void register(RegistrationMessage message) {
		grid.handleRegistrationMessage(message);
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/file/{id}")
	public Attachment getFile(@PathParam("id")String fileId) throws IOException {
		return fileManager.getFile(fileId);
	}
}
