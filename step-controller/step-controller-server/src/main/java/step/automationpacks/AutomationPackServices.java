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
package step.automationpacks;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import step.core.deployment.AbstractStepAsyncServices;
import step.framework.server.security.Secured;
import step.framework.server.tables.service.TableService;
import step.functions.accessor.FunctionAccessor;

import java.io.IOException;
import java.io.InputStream;

@Singleton
@Path("automationpacks")
@Tag(name = "AutomationPackages")
public class AutomationPackServices extends AbstractStepAsyncServices {

    private AutomationPackService service;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        service = new AutomationPackService(getContext().get(FunctionAccessor.class));
    }

    @POST
    @Secured
    @Path("/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public String uploadPackage(@FormDataParam("file") InputStream archive,
                                                @FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException, AutomationPackPreparationException {
        return service.uploadNewPackage(archive);
    }
}
