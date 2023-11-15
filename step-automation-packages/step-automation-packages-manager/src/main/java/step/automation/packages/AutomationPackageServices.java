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
package step.automation.packages;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import step.core.deployment.AbstractStepServices;
import step.framework.server.security.Secured;

import java.io.InputStream;

@Path("/automation-packages")
@Tag(name = "Automation packages")
public class AutomationPackageServices extends AbstractStepServices {

    protected AutomationPackageManager automationPackageManager;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        automationPackageManager = getContext().get(AutomationPackageManager.class);
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-read")
    public AutomationPackage getAutomationPackage(@PathParam("name") String automationPackageName) {
        return automationPackageManager.getAutomationPackageByName(automationPackageName);
    }

    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-delete")
    public void deleteAutomationPackage(@PathParam("name") String automationPackageName) {
        automationPackageManager.removeAutomationPackage(automationPackageName);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Secured(right = "automation-package-write")
    public String createAutomationPackage(@FormDataParam("file") InputStream uploadedInputStream,
                                          @FormDataParam("file") FormDataContentDisposition fileDetail,
                                          @Context UriInfo uriInfo) throws Exception {
        return automationPackageManager.createAutomationPackage(uploadedInputStream, fileDetail.getFileName(), getObjectEnricher());

    }

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Secured(right = "automation-package-write")
    public Response updateAutomationPackage(@FormDataParam("file") InputStream uploadedInputStream,
                                          @FormDataParam("file") FormDataContentDisposition fileDetail,
                                          @Context UriInfo uriInfo) throws Exception {
        AutomationPackageManager.PackageUpdateResult result = automationPackageManager.createOrUpdateAutomationPackage(true, uploadedInputStream, fileDetail.getFileName(), getObjectEnricher());
        Response.ResponseBuilder responseBuilder;
        switch (result.getStatus()){
            case CREATED:
                responseBuilder = Response.status(201);
                break;
            default:
                responseBuilder = Response.status(200);
                break;
        }
        return responseBuilder.entity(result.getId()).build();
    }

}