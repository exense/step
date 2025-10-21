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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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
import org.xml.sax.SAXException;
import step.automation.packages.execution.AutomationPackageExecutor;
import step.controller.services.async.AsyncTaskStatus;
import step.core.access.User;
import step.core.accessors.AbstractOrganizableObject;
import step.core.deployment.AbstractStepAsyncServices;
import step.core.deployment.ControllerServiceException;
import step.core.execution.model.AutomationPackageExecutionParameters;
import step.core.execution.model.IsolatedAutomationPackageExecutionParameters;
import step.core.maven.MavenArtifactIdentifier;
import step.core.maven.MavenArtifactIdentifierFromXmlParser;
import step.framework.server.audit.AuditLogger;
import step.framework.server.security.Secured;
import step.framework.server.tables.service.TableService;
import step.framework.server.tables.service.bulk.TableBulkOperationReport;
import step.framework.server.tables.service.bulk.TableBulkOperationRequest;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.Map;

@Path("/automation-packages")
@Tag(name = "Automation packages")
public class AutomationPackageServices extends AbstractStepAsyncServices {

    protected AutomationPackageManager automationPackageManager;
    protected AutomationPackageExecutor automationPackageExecutor;
    protected TableService tableService;
    protected XmlMapper xmlMapper;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        automationPackageManager = getContext().get(AutomationPackageManager.class);
        automationPackageExecutor = getContext().get(AutomationPackageExecutor.class);
        tableService = getContext().require(TableService.class);
        xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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

    private void auditLog(String operation, ObjectId apId) {
        if (apId != null) {
            auditLog(operation, apId.toString());
        }
    }

    private void auditLog(String operation, String apId) {
        if (apId != null && AuditLogger.isEntityModificationsLoggingEnabled()) {
            try {
                auditLog(operation, getAutomationPackage(apId));
            } catch (Exception ignored) {
                // not expected to fail, but let's not crash in case it does
            }
        }
    }

    private void auditLog(String operation, AutomationPackage ap) {
        if (ap != null) {
            AuditLogger.logEntityModification(getHttpSession(), operation, "automation-packages", ap);
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-delete")
    public void deleteAutomationPackage(@PathParam("id") String id) {
        deleteSingleAutomationPackage(id);
    }

    private void deleteSingleAutomationPackage(String id) {
        try {
            AutomationPackage automationPackage = getAutomationPackage(id);
            assertEntityIsEditableInContext(automationPackage);
            automationPackageManager.removeAutomationPackage(new ObjectId(id), getObjectPredicate());
            auditLog("delete",  automationPackage);
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
            auditLog("create",  id);
            return id == null ? null : id.toString();
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
     *     <classifier>tests</scope>
     * </dependency>
     */
    @POST
    @Path("/mvn")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Secured(right = "automation-package-write")
    public String createAutomationPackageFromMaven(@QueryParam("version") String apVersion,
                                                   @QueryParam("activationExpr") String activationExpression,
                                                   @RequestBody() String mavenArtifactXml) {
        try {
            MavenArtifactIdentifier mavenArtifactIdentifier = getMavenArtifactIdentifierFromXml(mavenArtifactXml);
            ObjectId id = automationPackageManager.createAutomationPackageFromMaven(
                    mavenArtifactIdentifier, apVersion, activationExpression, getObjectEnricher(), getObjectPredicate()
            );
            auditLog("create-from-maven",  id);
            return id == null ? null : id.toString();
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException("Cannot parse the maven artifact xml", e);
        }
    }

    protected MavenArtifactIdentifier getMavenArtifactIdentifierFromXml(String mavenArtifactXml) throws ParserConfigurationException, IOException, SAXException {
        return new MavenArtifactIdentifierFromXmlParser(xmlMapper).parse(mavenArtifactXml);
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
            auditLog("update-metadata", id);
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public AutomationPackageUpdateResult updateAutomationPackage(@PathParam("id") String id,
                                                                 @QueryParam("async") Boolean async,
                                                                 @QueryParam("version") String apVersion,
                                                                 @QueryParam("activationExpr") String activationExpression,
                                                                 @FormDataParam("file") InputStream uploadedInputStream,
                                                                 @FormDataParam("file") FormDataContentDisposition fileDetail) {
        checkAutomationPackageAcceptable(id);
        try {
            var result = automationPackageManager.createOrUpdateAutomationPackage(
                    true, false, new ObjectId(id),
                    uploadedInputStream, fileDetail.getFileName(), apVersion, activationExpression,
                    getObjectEnricher(), getObjectPredicate(), async != null && async
            );
            auditLog("update", result.getId());
            return result;
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
            assertEntityIsEditableInContext(automationPackage);
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
            auditLog("create-or-update", result.getId());
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
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public AutomationPackageUpdateResult createOrUpdateAutomationPackageFromMaven(@QueryParam("async") Boolean async,
                                                                                  @QueryParam("version") String apVersion,
                                                                                  @QueryParam("activationExpr") String activationExpression,
                                                                                  @RequestBody() String mavenArtifactXml) {
        try {
            MavenArtifactIdentifier mvnIdentifier = getMavenArtifactIdentifierFromXml(mavenArtifactXml);
            var result = automationPackageManager.createOrUpdateAutomationPackageFromMaven(
                    mvnIdentifier, true, true, null, apVersion, activationExpression, getObjectEnricher(), getObjectPredicate(), async == null ? false : async
            );
            auditLog("create-or-update-maven", result.getId());
            return result;
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException("Cannot parse the maven artifact xml", e);
        }
    }

    /**
     * @param mavenArtifactXml Example:
     *                         <dependency>
     *                         <groupId>junit</groupId>
     *                         <artifactId>junit</artifactId>
     *                         <version>4.13.2</version>
     *                         <scope>test</scope>
     *                         </dependency>
     */
    @PUT
    @Path("/{id}/mvn")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public AutomationPackageUpdateResult updateAutomationPackageFromMaven(@PathParam("id") String id,
                                                                          @QueryParam("async") Boolean async,
                                                                          @QueryParam("version") String apVersion,
                                                                          @QueryParam("activationExpr") String activationExpression,
                                                                          @RequestBody() String mavenArtifactXml) {
        try {
            MavenArtifactIdentifier mvnIdentifier = getMavenArtifactIdentifierFromXml(mavenArtifactXml);
            var result = automationPackageManager.createOrUpdateAutomationPackageFromMaven(
                    mvnIdentifier, true, false, new ObjectId(id), apVersion,
                    activationExpression, getObjectEnricher(), getObjectPredicate(), async == null ? false : async
            );
            auditLog("update-maven", result.getId());
            return result;
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

    @GET
    @Path("{id}/entities")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<? extends AbstractOrganizableObject>> listEntities(@PathParam("id") String id){
        try {
            return automationPackageManager.getAllEntities(new ObjectId(id));
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    @POST
    @Path("/bulk/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-delete")
    public AsyncTaskStatus<TableBulkOperationReport> bulkDelete(TableBulkOperationRequest request) {
        Consumer<String> consumer = this::deleteSingleAutomationPackage;
        return scheduleAsyncTaskWithinSessionContext(h ->
                tableService.performBulkOperation(AutomationPackageEntity.entityName, request, consumer, getSession()));
    }

}