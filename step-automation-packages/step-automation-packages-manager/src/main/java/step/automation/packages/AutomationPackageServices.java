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
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import step.automation.packages.execution.AutomationPackageExecutionParameters;
import step.automation.packages.execution.AutomationPackageExecutor;
import step.core.access.User;
import step.core.deployment.AbstractStepServices;
import step.framework.server.security.Secured;

import java.io.InputStream;
import java.util.List;

@Path("/automation-packages")
@Tag(name = "Automation packages")
public class AutomationPackageServices extends AbstractStepServices {

    protected AutomationPackageManager automationPackageManager;
    protected AutomationPackageExecutor automationPackageExecutor;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        automationPackageManager = getContext().get(AutomationPackageManager.class);
        automationPackageExecutor = getContext().get(AutomationPackageExecutor.class);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-read")
    public AutomationPackage getAutomationPackage(@PathParam("id") String id) {
        return automationPackageManager.getAutomatonPackageById(new ObjectId(id));
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-delete")
    public void deleteAutomationPackage(@PathParam("id") String id) {
        automationPackageManager.removeAutomationPackage(new ObjectId(id), getObjectPredicate());
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Secured(right = "automation-package-write")
    public String createAutomationPackage(@FormDataParam("file") InputStream automationPackageInputStream,
                                          @FormDataParam("file") FormDataContentDisposition fileDetail,
                                          @Context UriInfo uriInfo) throws Exception {
        ObjectId id = automationPackageManager.createAutomationPackage(automationPackageInputStream, fileDetail.getFileName(), getObjectEnricher(), getObjectPredicate());
        return id == null ? null : id.toString();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/execute")
    @Secured(right = "automation-package-execute")
    public List<String> executeAutomationPackage(@FormDataParam("file") InputStream automationPackageInputStream,
                                                 @FormDataParam("file") FormDataContentDisposition fileDetail,
                                                 @FormDataParam("executionParams") FormDataBodyPart executionParamsBodyPart,
                                                 @Context UriInfo uriInfo) throws Exception {
        AutomationPackageExecutionParameters executionParameters = null;
        if (executionParamsBodyPart != null) {
            // The workaround to parse execution parameters as application/json even if the Content-Type for this part is not explicitly set in request
            executionParamsBodyPart.setMediaType(MediaType.APPLICATION_JSON_TYPE);
            executionParameters = executionParamsBodyPart.getValueAs(AutomationPackageExecutionParameters.class);
        } else {
            executionParameters = new AutomationPackageExecutionParameters();
        }

        User user = getSession().getUser();
        return automationPackageExecutor.runInIsolation(
                automationPackageInputStream,
                fileDetail.getFileName(),
                executionParameters,
                getObjectEnricher(),
                user == null ? null : user.getId().toString(),
                getObjectPredicate()
        );
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Secured(right = "automation-package-write")
    public void updateAutomationPackageById(@PathParam("id") String id,
                                            @FormDataParam("file") InputStream uploadedInputStream,
                                            @FormDataParam("file") FormDataContentDisposition fileDetail,
                                            @Context UriInfo uriInfo) throws Exception {
        automationPackageManager.createOrUpdateAutomationPackage(
                true, false, new ObjectId(id),
                uploadedInputStream, fileDetail.getFileName(),
                getObjectEnricher(), getObjectPredicate()
        );
    }

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Secured(right = "automation-package-write")
    public Response createOrUpdateAutomationPackage(@FormDataParam("file") InputStream uploadedInputStream,
                                                    @FormDataParam("file") FormDataContentDisposition fileDetail,
                                                    @Context UriInfo uriInfo) throws Exception {
        AutomationPackageManager.PackageUpdateResult result = automationPackageManager.createOrUpdateAutomationPackage(
                true, true, null, uploadedInputStream, fileDetail.getFileName(),
                getObjectEnricher(), getObjectPredicate()
        );
        Response.ResponseBuilder responseBuilder;
        switch (result.getStatus()) {
            case CREATED:
                responseBuilder = Response.status(201);
                break;
            default:
                responseBuilder = Response.status(200);
                break;
        }
        return responseBuilder.entity(result.getId().toHexString()).build();
    }

}