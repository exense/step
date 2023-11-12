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
import jakarta.ws.rs.core.UriInfo;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.deployment.AbstractStepServices;
import step.framework.server.security.Secured;

import java.io.InputStream;

@Path("/autopackages")
@Tag(name = "Automation packages")
public class AutomationPackageServices extends AbstractStepServices {

    private static final Logger logger = LoggerFactory.getLogger(AutomationPackageServices.class);

    protected AutomationPackageManager automationPackageManager;

    @PostConstruct
    public void init() {
        automationPackageManager = getContext().get(AutomationPackageManager.class);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "autopack-read")
    public AutomationPackage getAutomationPackage(@PathParam("id") String automationPackageId) {
        return automationPackageManager.getAutomationPackage(automationPackageId);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "autopack-write")
    public void deleteAutomationPackage(@PathParam("id") String automationPackageId) {
        automationPackageManager.removeAutomationPackage(automationPackageId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "autopack-write")
    public String createAutomationPackage(@FormDataParam("file") InputStream uploadedInputStream,
                                          @FormDataParam("file") FormDataContentDisposition fileDetail,
                                          @Context UriInfo uriInfo) throws Exception {
        return automationPackageManager.createAutomationPackage(uploadedInputStream, fileDetail.getFileName(), getObjectEnricher());

    }

    /*
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/resourcebased")
    @Secured(right = "autopack-write")
    public String updateAutomationPackage(AutomationPackagePersistence automationPackage, @Context UriInfo uriInfo) throws Exception {
        throw new Exception(
                "This service has been removed. Use POST /rest/functionpackages/ instead. Lookup by resourceName isn't supported anymore");
    }

    */

    public static class PackagePreview {

        public PackagePreview(AutomationPackage automationPackage) {
            super();
            this.automationPackage = automationPackage;
        }

        public PackagePreview(String loadingError) {
            super();
            this.loadingError = loadingError;
        }

        private String loadingError;
        private AutomationPackage automationPackage;

        public String getLoadingError() {
            return loadingError;
        }

        public void setLoadingError(String loadingError) {
            this.loadingError = loadingError;
        }

        public AutomationPackage getFunctions() {
            return automationPackage;
        }

        public void setFunctions(AutomationPackage automationPackage) {
            this.automationPackage = automationPackage;
        }
    }
}