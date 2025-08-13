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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.api.client.http.HttpStatusCodes;
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
import step.automation.packages.client.model.AutomationPackageFromMavenRequest;
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

// TODO: improve the swagger doc for endpoint and params
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
            assertEntityIsAcceptableInContext(automationPackage);
            automationPackageManager.removeAutomationPackage(new ObjectId(id), getSession().getUser().getUsername(),getObjectPredicate());
        } catch (Exception e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    /**
     *
     * @param apVersion
     * @param activationExpression
     * @param automationPackageInputStream
     * @param fileDetail
     * @param apMavenSnippet
     * Example:
     * <dependency>
     *     <groupId>junit</groupId>
     *     <artifactId>junit</artifactId>
     *     <version>4.13.2</version>
     *     <scope>test</scope>
     *     <classifier>tests</scope>
     * </dependency>
     * @param keywordLibraryInputStream
     * @param keywordLibraryFileDetail
     * @param keywordLibraryMavenSnippet
     * @return
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Secured(right = "automation-package-write")
    public String createAutomationPackage(@QueryParam("version") String apVersion,
                                          @QueryParam("activationExpr") String activationExpression,
                                          @QueryParam("forceUpload") Boolean forceUpload,
                                          @FormDataParam("file") InputStream automationPackageInputStream,
                                          @FormDataParam("file") FormDataContentDisposition fileDetail,
                                          @FormDataParam("apMavenSnippet") String apMavenSnippet,
                                          @FormDataParam("keywordLibrary") InputStream keywordLibraryInputStream,
                                          @FormDataParam("keywordLibrary") FormDataContentDisposition keywordLibraryFileDetail,
                                          @FormDataParam("keywordLibraryMavenSnippet") String keywordLibraryMavenSnippet) {
        try {
            AutomationPackageFileSource apFileSource = getFileSource(automationPackageInputStream, fileDetail, apMavenSnippet, "Invalid maven snippet for automation package: ");
            AutomationPackageFileSource keywordLibrarySource = getFileSource(keywordLibraryInputStream, keywordLibraryFileDetail, keywordLibraryMavenSnippet, "Invalid maven snippet for keyword library: ");

            ObjectId id = automationPackageManager.createAutomationPackage(
                    apFileSource,
                    apVersion, activationExpression,
                    keywordLibrarySource, getUser(),
                    forceUpload == null ? false : forceUpload, true,
                    getObjectEnricher(), getObjectPredicate());
            return id == null ? null : id.toString();
        } catch (SameAutomationPackageOriginException e){
            throw new ControllerServiceException(HttpStatusCodes.STATUS_CODE_CONFLICT, e.getMessage());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    // TODO: remove after UI is switched to the universal endpoint
    /**
     * @param requestBody
     * Example:
     * <dependency>
     *     <groupId>junit</groupId>
     *     <artifactId>junit</artifactId>
     *     <version>4.13.2</version>
     *     <scope>test</scope>
     *     <classifier>tests</scope>
     * </dependency>
     */
    @Deprecated
    @POST
    @Path("/mvn")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Secured(right = "automation-package-write")
    public String createAutomationPackageFromMaven(@QueryParam("version") String apVersion,
                                                   @QueryParam("activationExpr") String activationExpression,
                                                   @RequestBody AutomationPackageFromMavenRequest requestBody) {
        try {
            MavenArtifactIdentifier mavenArtifactIdentifier = getMavenArtifactIdentifierFromXml(requestBody.getApMavenSnippetXml());
            AutomationPackageFileSource keywordLibrarySource = getKeywordLibrarySource(requestBody);
            return automationPackageManager.createAutomationPackageFromMaven(
                    mavenArtifactIdentifier, apVersion, activationExpression,
                    keywordLibrarySource,
                    getObjectEnricher(), getObjectPredicate(),
                    getUser(),
                    false, true).toString();
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException("Cannot parse the maven artifact xml", e);
        }
    }

    private AutomationPackageFileSource getKeywordLibrarySource(AutomationPackageFromMavenRequest requestBody) throws ParserConfigurationException, IOException, SAXException {
        AutomationPackageFileSource keywordLibrarySource = null;
        if(requestBody.getKeywordLibraryMavenSnippetXml() != null && !requestBody.getKeywordLibraryMavenSnippetXml().isEmpty()){
            keywordLibrarySource = AutomationPackageFileSource.withMavenIdentifier(getMavenArtifactIdentifierFromXml(requestBody.getKeywordLibraryMavenSnippetXml()));
        }
        return keywordLibrarySource;
    }

    protected MavenArtifactIdentifier getMavenArtifactIdentifierFromXml(String mavenArtifactXml) throws JsonProcessingException {
        return new MavenArtifactIdentifierFromXmlParser(xmlMapper).parse(mavenArtifactXml);
    }

    // TODO: tricky moment - the apMavenSnippet is defined in executionParams as 'originalRepositoryObject'
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/execute")
    @Secured(right = "automation-package-execute")
    public List<String> executeAutomationPackage(@FormDataParam("file") InputStream automationPackageInputStream,
                                                 @FormDataParam("file") FormDataContentDisposition fileDetail,
//                                                 @FormDataParam("apMavenSnippet") String apMavenSnippet,
                                                 @FormDataParam("keywordLibrary") InputStream keywordLibraryInputStream,
                                                 @FormDataParam("keywordLibrary") FormDataContentDisposition keywordLibraryFileDetail,
                                                 @FormDataParam("executionParams") FormDataBodyPart executionParamsBodyPart,
                                                 @FormDataParam("keywordLibraryMavenSnippet") String keywordLibraryMavenSnippet) {
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
            AutomationPackageFileSource keywordLibrarySource = getFileSource(keywordLibraryInputStream, keywordLibraryFileDetail, keywordLibraryMavenSnippet, "Invalid maven snippet for keyword library: ");
            return automationPackageExecutor.runInIsolation(
                    automationPackageInputStream,
                    fileDetail == null ? null : fileDetail.getFileName(),
                    executionParameters,
                    keywordLibrarySource, getUser(),
                    getObjectEnricher(),
                    getObjectPredicate());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    private String getUser() {
        return getSession().getUser().getUsername();
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
    public AutomationPackageUpdateResult updateAutomationPackage(@PathParam("id") String id,
                                                                 @QueryParam("async") Boolean async,
                                                                 @QueryParam("version") String apVersion,
                                                                 @QueryParam("activationExpr") String activationExpression,
                                                                 @QueryParam("forceUpload") Boolean forceUpload,
                                                                 @FormDataParam("file") InputStream uploadedInputStream,
                                                                 @FormDataParam("file") FormDataContentDisposition fileDetail,
                                                                 @FormDataParam("apMavenSnippet") String apMavenSnippet,
                                                                 @FormDataParam("keywordLibrary") InputStream keywordLibraryInputStream,
                                                                 @FormDataParam("keywordLibrary") FormDataContentDisposition keywordLibraryFileDetail,
                                                                 @FormDataParam("keywordLibraryMavenSnippet") String keywordLibraryMavenSnippet) {
        checkAutomationPackageAcceptable(id);
        try {
            AutomationPackageFileSource apFileSource = getFileSource(uploadedInputStream, fileDetail, apMavenSnippet, "Invalid maven snippet for automation package: ");
            AutomationPackageFileSource keywordLibrarySource = getFileSource(keywordLibraryInputStream, keywordLibraryFileDetail, keywordLibraryMavenSnippet, "Invalid maven snippet for keyword library: ");

            return automationPackageManager.createOrUpdateAutomationPackage(
                    true, false, new ObjectId(id),
                    apFileSource, keywordLibrarySource,
                    apVersion, activationExpression,
                    getObjectEnricher(), getObjectPredicate(), async != null && async, getUser(),
                    forceUpload == null ? false : forceUpload, true);
        } catch (SameAutomationPackageOriginException e) {
            throw new ControllerServiceException(HttpStatusCodes.STATUS_CODE_CONFLICT, e.getMessage());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    private AutomationPackageFileSource getFileSource(InputStream uploadedInputStream, FormDataContentDisposition fileDetail, String apMavenSnippet, String invalidSnippetErrorText) {
        AutomationPackageFileSource automationPackageFileSource = null;
        try {
            automationPackageFileSource = AutomationPackageFileSource.empty();
            if (uploadedInputStream != null) {
                automationPackageFileSource.addInputStream(uploadedInputStream, fileDetail == null ? null : fileDetail.getFileName());
            }
            if (apMavenSnippet != null) {
                automationPackageFileSource.addMavenIdentifier(getMavenArtifactIdentifierFromXml(apMavenSnippet));
            }
        } catch (JsonProcessingException e) {
            throw new ControllerServiceException(invalidSnippetErrorText + e.getMessage());
        }
        return automationPackageFileSource;
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
                                                    @QueryParam("forceUpload") Boolean forceUpload,
                                                    @QueryParam("activationExpr") String activationExpression,
                                                    @FormDataParam("file") InputStream uploadedInputStream,
                                                    @FormDataParam("file") FormDataContentDisposition fileDetail,
                                                    @FormDataParam("apMavenSnippet") String apMavenSnippet,
                                                    @FormDataParam("keywordLibrary") InputStream keywordLibraryInputStream,
                                                    @FormDataParam("keywordLibrary") FormDataContentDisposition keywordLibraryFileDetail,
                                                    @FormDataParam("keywordLibraryMavenSnippet") String keywordLibraryMavenSnippet) {
        try {
            AutomationPackageFileSource apFileSource = getFileSource(uploadedInputStream, fileDetail, apMavenSnippet, "Invalid maven snippet for automation package: ");
            AutomationPackageFileSource keywordLibrarySource = getFileSource(keywordLibraryInputStream, keywordLibraryFileDetail, keywordLibraryMavenSnippet, "Invalid maven snippet for keyword library: ");

            AutomationPackageUpdateResult result = automationPackageManager.createOrUpdateAutomationPackage(
                    true, true, null,
                    apFileSource,
                    keywordLibrarySource,
                    apVersion, activationExpression,
                    getObjectEnricher(), getObjectPredicate(), async != null && async, getUser(),
                    forceUpload == null ? false : forceUpload, true);
            Response.ResponseBuilder responseBuilder;
            if (result.getStatus() == AutomationPackageUpdateStatus.CREATED) {
                responseBuilder = Response.status(Response.Status.CREATED);
            } else {
                responseBuilder = Response.status(Response.Status.OK);
            }
            return responseBuilder.entity(result).build();
        } catch (SameAutomationPackageOriginException e){
            throw new ControllerServiceException(HttpStatusCodes.STATUS_CODE_CONFLICT, e.getMessage());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    // TODO: remove after UI is switched to the universal endpoint
    /**
     * Example:
     * <dependency>
     *     <groupId>junit</groupId>
     *     <artifactId>junit</artifactId>
     *     <version>4.13.2</version>
     *     <scope>test</scope>
     * </dependency>
     */
    @Deprecated
    @PUT
    @Path("/mvn")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public AutomationPackageUpdateResult createOrUpdateAutomationPackageFromMaven(@QueryParam("async") Boolean async,
                                                                                  @QueryParam("version") String apVersion,
                                                                                  @QueryParam("activationExpr") String activationExpression,
                                                                                  @RequestBody AutomationPackageFromMavenRequest requestBody) {
        try {
            MavenArtifactIdentifier mvnIdentifier = getMavenArtifactIdentifierFromXml(requestBody.getApMavenSnippetXml());
            AutomationPackageFileSource keywordLibrarySource = getKeywordLibrarySource(requestBody);
            return automationPackageManager.createOrUpdateAutomationPackageFromMaven(
                    mvnIdentifier, true, true, null, apVersion, activationExpression, keywordLibrarySource,
                    getObjectEnricher(), getObjectPredicate(),
                    async == null ? false : async, true, false
                    , getUser());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException("Cannot parse the maven artifact xml", e);
        }
    }

    // TODO: remove after UI is switched to the universal endpoint
    /**
     * Example:
     * <dependency>
     * <groupId>junit</groupId>
     * <artifactId>junit</artifactId>
     * <version>4.13.2</version>
     * <scope>test</scope>
     * </dependency>
     */
    @Deprecated
    @PUT
    @Path("/{id}/mvn")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public AutomationPackageUpdateResult updateAutomationPackageFromMaven(@PathParam("id") String id,
                                                                          @QueryParam("async") Boolean async,
                                                                          @QueryParam("version") String apVersion,
                                                                          @QueryParam("activationExpr") String activationExpression,
                                                                          @RequestBody AutomationPackageFromMavenRequest requestBody) {
        try {
            MavenArtifactIdentifier mvnIdentifier = getMavenArtifactIdentifierFromXml(requestBody.getApMavenSnippetXml());
            AutomationPackageFileSource keywordLibrarySource = getKeywordLibrarySource(requestBody);
            return automationPackageManager.createOrUpdateAutomationPackageFromMaven(
                    mvnIdentifier, true, false, new ObjectId(id), apVersion,
                    activationExpression, keywordLibrarySource, getObjectEnricher(), getObjectPredicate(), async == null ? false : async, true, false
                    , getUser());
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