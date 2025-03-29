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

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import step.automation.packages.execution.AutomationPackageExecutor;
import step.core.access.User;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.ControllerServiceException;
import step.core.execution.model.AutomationPackageExecutionParameters;
import step.core.execution.model.IsolatedAutomationPackageExecutionParameters;
import step.core.maven.MavenArtifactIdentifier;
import step.framework.server.security.Secured;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
        try {
            return automationPackageManager.getAutomatonPackageById(new ObjectId(id), getObjectPredicate());
        } catch (Exception e) {
            throw new ControllerServiceException(e.getMessage());
        }

    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-delete")
    public void deleteAutomationPackage(@PathParam("id") String id) {
        try {
            AutomationPackage automationPackage = getAutomationPackage(id);
            assertEntityIsAcceptableInContext(automationPackage);
            automationPackageManager.removeAutomationPackage(new ObjectId(id), getObjectPredicate());
        } catch (Exception e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Secured(right = "automation-package-write")
    public String createAutomationPackage(@QueryParam("version") String apVersion,
                                          @QueryParam("activationExpr") String activationExpression,
                                          @FormDataParam("file") InputStream automationPackageInputStream,
                                          @FormDataParam("file") FormDataContentDisposition fileDetail) {
        try {
            ObjectId id = automationPackageManager.createAutomationPackage(
                    automationPackageInputStream, fileDetail.getFileName(),
                    apVersion, activationExpression,
                    getObjectEnricher(), getObjectPredicate()
            );
            return id == null ? null : id.toString();
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    // TODO: better url?

    /**
     * @param mavenArtifactXml
     * Example:
     * <dependency>
     *     <groupId>junit</groupId>
     *     <artifactId>junit</artifactId>
     *     <version>4.13.2</version>
     *     <scope>test</scope>
     *     <classifier>tests</scope>
     * </dependency>
     */
    @POST
    @Path("/mvn")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Secured(right = "automation-package-write")
    public String createAutomationPackageFromMaven(@QueryParam("activationExpr") String activationExpression,
                                                   @RequestBody() String mavenArtifactXml) {
        try {
            MavenArtifactIdentifier mavenArtifactIdentifier = getMavenArtifactIdentifierFromXml(mavenArtifactXml);
            return automationPackageManager.createAutomationPackageFromMaven(
                    mavenArtifactIdentifier, activationExpression, getObjectEnricher(), getObjectPredicate()
            ).toString();
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException("Cannot parse the maven artifact xml", e);
        }
    }

    protected MavenArtifactIdentifier getMavenArtifactIdentifierFromXml(String mavenArtifactXml) throws ParserConfigurationException, IOException, SAXException {
        return new MavenArtifactIdentifierFromXmlParser().parse(mavenArtifactXml);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/execute")
    @Secured(right = "automation-package-execute")
    public List<String> executeAutomationPackage(@FormDataParam("file") InputStream automationPackageInputStream,
                                                 @FormDataParam("file") FormDataContentDisposition fileDetail,
                                                 @FormDataParam("executionParams") FormDataBodyPart executionParamsBodyPart) {
        IsolatedAutomationPackageExecutionParameters executionParameters;
        if (executionParamsBodyPart != null) {
            // The workaround to parse execution parameters as application/json even if the Content-Type for this part is not explicitly set in request
            executionParamsBodyPart.setMediaType(MediaType.APPLICATION_JSON_TYPE);
            executionParameters = executionParamsBodyPart.getValueAs(IsolatedAutomationPackageExecutionParameters.class);
        } else {
            executionParameters = new IsolatedAutomationPackageExecutionParameters();
        }

        // in executionParameters we can define the user 'onBehalfOf'
        // if this user is not defined, the user from session is taken
        checkRightsOnBehalfOf("automation-package-execute", executionParameters.getUserID());
        if (executionParameters.getUserID() == null) {
            User user = getSession().getUser();
            if (user != null) {
                executionParameters.setUserID(user.getUsername());
            }
        }

        try {
            return automationPackageExecutor.runInIsolation(
                    automationPackageInputStream,
                    fileDetail == null ? null : fileDetail.getFileName(),
                    executionParameters,
                    getObjectEnricher(),
                    getObjectPredicate()
            );
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/execute/{id}")
    @Secured(right = "automation-package-execute")
    public List<String> executeDeployedAutomationPackage(@PathParam("id") String automationPackageId,
                                                         AutomationPackageExecutionParameters executionParameters) {
        // in executionParameters we can define the user 'onBehalfOf'
        // if this user is not defined, the user from session is taken
        checkRightsOnBehalfOf("automation-package-execute", executionParameters.getUserID());
        if (executionParameters.getUserID() == null) {
            User user = getSession().getUser();
            if (user != null) {
                executionParameters.setUserID(user.getUsername());
            }
        }

        try {
            return automationPackageExecutor.runDeployedAutomationPackage(
                    new ObjectId(automationPackageId),
                    executionParameters,
                    getObjectEnricher(),
                    getObjectPredicate()
            );
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    @PUT
    @Path("/{id}/metadata")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public void updateAutomationPackageMetadata(@PathParam("id") String id,
                                                @QueryParam("activationExpr") String activationExpression,
                                                @QueryParam("version") String apVersion) {
        checkAutomationPackageAcceptable(id);
        try {
            automationPackageManager.updateAutomationPackageMetadata(new ObjectId(id), apVersion, activationExpression, getObjectPredicate());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public AutomationPackageUpdateResult updateAutomationPackageMetadata(@PathParam("id") String id,
                                                                         @QueryParam("async") Boolean async,
                                                                         @QueryParam("version") String apVersion,
                                                                         @QueryParam("activationExpr") String activationExpression,
                                                                         @FormDataParam("file") InputStream uploadedInputStream,
                                                                         @FormDataParam("file") FormDataContentDisposition fileDetail) {
        checkAutomationPackageAcceptable(id);
        try {
            return automationPackageManager.createOrUpdateAutomationPackage(
                    true, false, new ObjectId(id),
                    uploadedInputStream, fileDetail.getFileName(), apVersion, activationExpression,
                    getObjectEnricher(), getObjectPredicate(), async != null && async
            );
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    private void checkAutomationPackageAcceptable(String id) {
        AutomationPackage automationPackage = null;
        try {
            automationPackage = getAutomationPackage(id);
        } catch (Exception e) {
            //getAutomationPackage throws exception if the package doesn't exist, whether this is an errors is managed in below createOrUpdateAutomationPackage
        }
        if (automationPackage != null) {
            assertEntityIsAcceptableInContext(automationPackage);
        }
    }

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public Response createOrUpdateAutomationPackage(@QueryParam("async") Boolean async,
                                                    @QueryParam("version") String apVersion,
                                                    @QueryParam("activationExpr") String activationExpression,
                                                    @FormDataParam("file") InputStream uploadedInputStream,
                                                    @FormDataParam("file") FormDataContentDisposition fileDetail) {
        try {
            AutomationPackageUpdateResult result = automationPackageManager.createOrUpdateAutomationPackage(
                    true, true, null, uploadedInputStream, fileDetail.getFileName(), apVersion, activationExpression,
                    getObjectEnricher(), getObjectPredicate(), async != null && async
            );
            Response.ResponseBuilder responseBuilder;
            if (result.getStatus() == AutomationPackageUpdateStatus.CREATED) {
                responseBuilder = Response.status(Response.Status.CREATED);
            } else {
                responseBuilder = Response.status(Response.Status.OK);
            }
            return responseBuilder.entity(result).build();
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    /**
     * @param mavenArtifactXml
     * Example:
     * <dependency>
     *     <groupId>junit</groupId>
     *     <artifactId>junit</artifactId>
     *     <version>4.13.2</version>
     *     <scope>test</scope>
     * </dependency>
     */
    @PUT
    @Path("/mvn")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Secured(right = "automation-package-write")
    public String createOrUpdateAutomationPackageFromMaven(@QueryParam("async") Boolean async,
                                                           @QueryParam("activationExpr") String activationExpression,
                                                           @RequestBody() String mavenArtifactXml) {
        try {
            MavenArtifactIdentifier mvnIdentifier = getMavenArtifactIdentifierFromXml(mavenArtifactXml);
            return automationPackageManager.createOrUpdateAutomationPackageFromMaven(
                    mvnIdentifier, true, true, null, activationExpression, getObjectEnricher(), getObjectPredicate(), async
            ).toString();
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException("Cannot parse the maven artifact xml", e);
        }
    }

    @GET
    @Path("/schema")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAutomationPackageDescriptorSchema() {
        return automationPackageManager.getDescriptorJsonSchema();
    }

}