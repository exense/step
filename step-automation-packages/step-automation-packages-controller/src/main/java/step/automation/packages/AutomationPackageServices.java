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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import step.attachments.FileResolver;
import step.automation.packages.execution.AutomationPackageExecutor;
import step.controller.services.async.AsyncTaskStatus;
import step.core.access.User;
import step.core.accessors.AbstractOrganizableObject;
import step.core.deployment.AbstractStepAsyncServices;
import step.core.deployment.ControllerServiceException;
import step.core.entities.EntityManager;
import step.core.execution.model.AutomationPackageExecutionParameters;
import step.core.execution.model.IsolatedAutomationPackageExecutionParameters;
import step.core.maven.MavenArtifactIdentifier;
import step.core.maven.MavenArtifactIdentifierFromXmlParser;
import step.framework.server.audit.AuditLogger;
import step.framework.server.security.Secured;
import step.framework.server.tables.service.TableService;
import step.framework.server.tables.service.bulk.BulkOperationWarningException;
import step.framework.server.tables.service.bulk.TableBulkOperationReport;
import step.framework.server.tables.service.bulk.TableBulkOperationRequest;
import step.resources.Resource;
import step.resources.ResourceManager;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.Map;

@Path("/automation-packages")
@Tag(name = "Automation packages")
public class AutomationPackageServices extends AbstractStepAsyncServices {

    public static final String PLANS_ATTRIBUTES = "plansAttributes";
    public static final String FUNCTIONS_ATTRIBUTES = "functionsAttributes";
    public static final String TOKEN_SELECTION_CRITERIA = "tokenSelectionCriteria";
    public static final String EXECUTE_FUNCTIONS_LOCALLY = "executeFunctionsLocally";
    protected AutomationPackageManager automationPackageManager;
    protected AutomationPackageExecutor automationPackageExecutor;
    protected TableService tableService;
    protected XmlMapper xmlMapper;

    private static final String COLLISION_ERROR_NAME = "Automation Package Conflict";

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
            AuditLogger.logEntityModification(getSession(), operation, "automation-packages", ap, getObjectEnricher());
        }
    }

    private void auditLog(String operation, Resource resource) {
        if (resource != null) {
            AuditLogger.logEntityModification(getHttpSession(), operation, "automation-packages-resources", resource, getObjectEnricher());
        }
    }

    private void auditLogForResource(String operation, String resourceId) {
        if (resourceId != null && AuditLogger.isEntityModificationsLoggingEnabled()) {
            try {
                auditLog(operation, automationPackageManager.getResourceManager().getResource(resourceId));
            } catch (Exception ignored) {
                // not expected to fail, but let's not crash in case it does
            }
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
            automationPackageManager.removeAutomationPackage(new ObjectId(id),
                    getSession().getUser().getUsername(),
                    getObjectPredicate(), getWriteAccessValidator());
            auditLog("delete",  automationPackage);
        } catch (AutomationPackageAccessException ex){
            throw new ControllerServiceException(HttpStatus.SC_FORBIDDEN, ex.getMessage());
        } catch (Exception e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    /**
     * Convenience service to only allow creation of a new package
     * @param apVersion the version to be set for this AP
     * @param activationExpression activation expression to selected entities of this AP during executions
     * @param forceRefreshOfSnapshots if other AP would be modified (because they use the same snapshot artefacts that has a new version) this flag must be true to allow the update
     * @param automationPackageInputStream AP package file as input stream
     * @param fileDetail FormDataContentDisposition of the file when provided as inputstream
     * @param apMavenSnippet maven snippet of the package when deploying from a maven repository
     *       Example:
     *      <dependency>
     *           <groupId>junit</groupId>
     *          <artifactId>junit</artifactId>
     *           <version>4.13.2</version>
     *           <scope>test</scope>
     *           <classifier>tests</scope>
     *       </dependency>

     * @param apLibraryInputStream AP library file as input stream
     * @param apLibraryFileDetail FormDataContentDisposition of the library file when provided as input stream
     * @param apLibraryMavenSnippet maven snippet of the library when deploying from a maven repository
     * @param apResourceId id of the package resource when deploying with an existing resource
     * @param apLibraryResourceId id of the library resource when deploying with an existing resource
     * @param plansAttributesAsString Serialized Map of plan attributes
     * @param functionsAttributesAsString     Serialized Map of function attributes
     * @param tokenSelectionCriteriaAsString Serialized Map of functions token selection criteria
     * @param executeFunctionsLocally whether functions should be executed locally
     * @return the ID of the created AP
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public AutomationPackageUpdateResult createAutomationPackage(@FormDataParam("versionName") String apVersion,
                                          @FormDataParam("activationExpr") String activationExpression,
                                          @FormDataParam("forceRefreshOfSnapshots") boolean forceRefreshOfSnapshots,
                                          @FormDataParam("file") InputStream automationPackageInputStream,
                                          @FormDataParam("file") FormDataContentDisposition fileDetail,
                                          @FormDataParam("apMavenSnippet") String apMavenSnippet,
                                          @FormDataParam("apResourceId") String apResourceId,
                                          @FormDataParam("apLibrary") InputStream apLibraryInputStream,
                                          @FormDataParam("apLibrary") FormDataContentDisposition apLibraryFileDetail,
                                          @FormDataParam("apLibraryMavenSnippet") String apLibraryMavenSnippet,
                                          @FormDataParam("apLibraryResourceId") String apLibraryResourceId,
                                          @FormDataParam("managedLibraryName") String managedLibraryName,
                                          @FormDataParam(PLANS_ATTRIBUTES) String plansAttributesAsString,
                                          @FormDataParam(FUNCTIONS_ATTRIBUTES) String functionsAttributesAsString,
                                          @FormDataParam(TOKEN_SELECTION_CRITERIA) String tokenSelectionCriteriaAsString,
                                          @FormDataParam(EXECUTE_FUNCTIONS_LOCALLY) boolean executeFunctionsLocally) {
        try {
            ParsedRequestParameters parsedRequestParameters = getParsedRequestParameters(automationPackageInputStream, fileDetail,
                    apMavenSnippet, apLibraryInputStream, apLibraryFileDetail, apLibraryMavenSnippet, apResourceId, apLibraryResourceId,
                    managedLibraryName, plansAttributesAsString, functionsAttributesAsString, tokenSelectionCriteriaAsString);

            AutomationPackageUpdateParameter parameters = getAutomationPackageUpdateParameterBuilder()
                    .withAllowCreate(true).withAllowUpdate(false).withAsync(false)
                    .withForceRefreshOfSnapshots(forceRefreshOfSnapshots).withCheckForSameOrigin(true)
                    .withApSource(parsedRequestParameters.apFileSource).withApLibrarySource(parsedRequestParameters.apLibrarySource)
                    .withVersionName(apVersion).withActivationExpression(activationExpression)
                    .withPlansAttributes(parsedRequestParameters.plansAttributes).withFunctionsAttributes(parsedRequestParameters.functionsAttributes)
                    .withTokenSelectionCriteria(parsedRequestParameters.tokenSelectionCriteria).withExecuteFunctionsLocally(executeFunctionsLocally)
                    .build();
            AutomationPackageUpdateResult result = automationPackageManager.createOrUpdateAutomationPackage(parameters);
            ObjectId id = result.getId();
            auditLog("create",  id);
            return result;
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage(), e);
        }
    }

    private AutomationPackageUpdateParameterBuilder getAutomationPackageUpdateParameterBuilder() {
        return new AutomationPackageUpdateParameterBuilder()
                .withEnricher(getObjectEnricher())
                .withObjectPredicate(getObjectPredicate())
                .withWriteAccessValidator(getWriteAccessValidator())
                .withActorUser(getUser());
    }

    protected MavenArtifactIdentifier getMavenArtifactIdentifierFromXml(String mavenArtifactXml) throws JsonProcessingException {
        return new MavenArtifactIdentifierFromXmlParser(xmlMapper).parse(mavenArtifactXml);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/execute")
    @Secured(right = "automation-package-execute")
    public List<String> executeAutomationPackage(@FormDataParam("file") InputStream automationPackageInputStream,
                                                 @FormDataParam("file") FormDataContentDisposition fileDetail,
                                                 @FormDataParam("apMavenSnippet") String apMavenSnippet,
                                                 @FormDataParam("apResourceId") String apResourceId,
                                                 @FormDataParam("apLibrary") InputStream apLibraryInputStream,
                                                 @FormDataParam("apLibrary") FormDataContentDisposition apLibraryFileDetail,
                                                 @FormDataParam("apLibraryMavenSnippet") String apLibraryMavenSnippet,
                                                 @FormDataParam("apLibraryResourceId") String apLibraryResourceId,
                                                 @FormDataParam("managedLibraryName") String managedLibraryName,
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
            AutomationPackageFileSource apLibrarySource = getLibraryFileSource(apLibraryInputStream, apLibraryFileDetail, apLibraryMavenSnippet,
                    apLibraryResourceId, managedLibraryName);
            AutomationPackageFileSource apSource = getApFileSource(automationPackageInputStream, fileDetail, apMavenSnippet, apResourceId);

            return automationPackageExecutor.runInIsolation(
                    apSource,
                    executionParameters,
                    apLibrarySource, getUser(),
                    getObjectEnricher(),
                    getObjectPredicate());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage(), e);
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
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public AutomationPackageUpdateResult updateAutomationPackage(@PathParam("id") String id,
                                                                 @FormDataParam("async") boolean async,
                                                                 @FormDataParam("versionName") String apVersion,
                                                                 @FormDataParam("activationExpr") String activationExpression,
                                                                 @FormDataParam("forceRefreshOfSnapshots") boolean forceRefreshOfSnapshots,
                                                                 @FormDataParam("file") InputStream uploadedInputStream,
                                                                 @FormDataParam("file") FormDataContentDisposition fileDetail,
                                                                 @FormDataParam("apMavenSnippet") String apMavenSnippet,
                                                                 @FormDataParam("apLibrary") InputStream apLibraryInputStream,
                                                                 @FormDataParam("apLibrary") FormDataContentDisposition apLibraryFileDetail,
                                                                 @FormDataParam("apLibraryMavenSnippet") String apLibraryMavenSnippet,
                                                                 @FormDataParam("apResourceId") String apResourceId,
                                                                 @FormDataParam("apLibraryResourceId") String apLibraryResourceId,
                                                                 @FormDataParam("managedLibraryName") String managedLibraryName,
                                                                 @FormDataParam(PLANS_ATTRIBUTES) String plansAttributesAsString,
                                                                 @FormDataParam(FUNCTIONS_ATTRIBUTES) String functionsAttributesAsString,
                                                                 @FormDataParam(TOKEN_SELECTION_CRITERIA) String tokenSelectionCriteriaAsString,
                                                                 @FormDataParam(EXECUTE_FUNCTIONS_LOCALLY) boolean executeFunctionsLocally) {
        try {
            checkAutomationPackageAcceptable(id);
            ParsedRequestParameters parsedRequestParameters = getParsedRequestParameters(uploadedInputStream, fileDetail, apMavenSnippet, apLibraryInputStream, apLibraryFileDetail,
                    apLibraryMavenSnippet, apResourceId, apLibraryResourceId, managedLibraryName, plansAttributesAsString, functionsAttributesAsString, tokenSelectionCriteriaAsString);

            AutomationPackageUpdateParameter updateParameters = getAutomationPackageUpdateParameterBuilder().withAllowCreate(false).withExplicitOldId(new ObjectId(id))
                    .withApSource(parsedRequestParameters.apFileSource).withApLibrarySource(parsedRequestParameters.apLibrarySource)
                    .withVersionName(apVersion).withActivationExpression(activationExpression)
                    .withAsync(async).withForceRefreshOfSnapshots(forceRefreshOfSnapshots)
                    .withPlansAttributes(parsedRequestParameters.plansAttributes).withFunctionsAttributes(parsedRequestParameters.functionsAttributes)
                    .withTokenSelectionCriteria(parsedRequestParameters.tokenSelectionCriteria).withExecuteFunctionsLocally(executeFunctionsLocally)
                    .build();
            AutomationPackageUpdateResult result = automationPackageManager.createOrUpdateAutomationPackage(updateParameters);
            auditLog("update", result.getId());
            return result;
        } catch (AutomationPackageAccessException ex) {
            throw new ControllerServiceException(HttpStatus.SC_FORBIDDEN, ex.getMessage());
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

    private AutomationPackageFileSource getApFileSource(InputStream uploadedInputStream, FormDataContentDisposition fileDetail,
                                                      String apMavenSnippet, String resourceIdPath) {
        return getFileSource(uploadedInputStream, fileDetail, apMavenSnippet, "Invalid maven snippet for automation package: ", resourceIdPath, null);
    }

    private AutomationPackageFileSource getLibraryFileSource(InputStream uploadedInputStream, FormDataContentDisposition fileDetail,
                                                             String apMavenSnippet, String resourceIdPath,
                                                             String managedLibraryName) {
        return getFileSource(uploadedInputStream, fileDetail, apMavenSnippet, "Invalid maven snippet for automation package library: ", resourceIdPath, managedLibraryName);
    }

    private AutomationPackageFileSource getFileSource(InputStream uploadedInputStream, FormDataContentDisposition fileDetail,
                                                      String apMavenSnippet, String invalidSnippetErrorText, String resourceIdPath,
                                                      String managedLibraryName) {

        try {
            AutomationPackageFileSource automationPackageFileSource = AutomationPackageFileSource.empty();
            if (uploadedInputStream != null) {
                automationPackageFileSource.setInputStream(uploadedInputStream, fileDetail == null ? null : fileDetail.getFileName());
            } else if (apMavenSnippet != null) {
                automationPackageFileSource.setMavenIdentifier(getMavenArtifactIdentifierFromXml(apMavenSnippet));
            } else if (resourceIdPath != null) {
                if (!FileResolver.isResource(resourceIdPath)) {
                    throw new ControllerServiceException("Invalid resource path: '"+ resourceIdPath + "' Resource paths must be given in the format 'resource:<id>'.");
                }
                automationPackageFileSource.setResourceId(FileResolver.resolveResourceId(resourceIdPath));
            } else if (managedLibraryName != null) {
                automationPackageFileSource.setManagedLibraryKey(managedLibraryName);
            }
            return automationPackageFileSource;
        } catch (JsonProcessingException e) {
            throw new ControllerServiceException(invalidSnippetErrorText + e.getMessage());
        }
    }

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public Response createOrUpdateAutomationPackage(@FormDataParam("async") boolean async,
                                                    @FormDataParam("versionName") String apVersion,
                                                    @FormDataParam("forceRefreshOfSnapshots") boolean forceRefreshOfSnapshots,
                                                    @FormDataParam("activationExpr") String activationExpression,
                                                    @FormDataParam("file") InputStream uploadedInputStream,
                                                    @FormDataParam("file") FormDataContentDisposition fileDetail,
                                                    @FormDataParam("apMavenSnippet") String apMavenSnippet,
                                                    @FormDataParam("apLibrary") InputStream apLibraryInputStream,
                                                    @FormDataParam("apLibrary") FormDataContentDisposition apLibraryFileDetail,
                                                    @FormDataParam("apLibraryMavenSnippet") String apLibraryMavenSnippet,
                                                    @FormDataParam("apResourceId") String apResourceId,
                                                    @FormDataParam("apLibraryResourceId") String apLibraryResourceId,
                                                    @FormDataParam("managedLibraryName") String managedLibraryName,
                                                    @FormDataParam(PLANS_ATTRIBUTES) String plansAttributesAsString,
                                                    @FormDataParam(FUNCTIONS_ATTRIBUTES) String functionsAttributesAsString,
                                                    @FormDataParam(TOKEN_SELECTION_CRITERIA) String tokenSelectionCriteriaAsString,
                                                    @FormDataParam(EXECUTE_FUNCTIONS_LOCALLY) boolean executeFunctionsLocally) {
        try {
            ParsedRequestParameters parsedRequestParameters = getParsedRequestParameters(uploadedInputStream, fileDetail, apMavenSnippet, apLibraryInputStream,
                    apLibraryFileDetail, apLibraryMavenSnippet, apResourceId, apLibraryResourceId, managedLibraryName,
                    plansAttributesAsString, functionsAttributesAsString, tokenSelectionCriteriaAsString);

            AutomationPackageUpdateParameter updateParameters = getAutomationPackageUpdateParameterBuilder()
                    .withApSource(parsedRequestParameters.apFileSource).withApLibrarySource(parsedRequestParameters.apLibrarySource)
                    .withVersionName(apVersion).withActivationExpression(activationExpression)
                    .withAsync(async).withForceRefreshOfSnapshots(forceRefreshOfSnapshots)
                    .withPlansAttributes(parsedRequestParameters.plansAttributes).withFunctionsAttributes(parsedRequestParameters.functionsAttributes)
                    .withTokenSelectionCriteria(parsedRequestParameters.tokenSelectionCriteria).withExecuteFunctionsLocally(executeFunctionsLocally)
                    .build();
            AutomationPackageUpdateResult result = automationPackageManager.createOrUpdateAutomationPackage(updateParameters);
            auditLog("create-or-update", result.getId());
            Response.ResponseBuilder responseBuilder;
            if (result.getStatus() == AutomationPackageUpdateStatus.CREATED) {
                responseBuilder = Response.status(Response.Status.CREATED);
            } else {
                responseBuilder = Response.status(Response.Status.OK);
            }
            return responseBuilder.entity(result).build();
        } catch (AutomationPackageAccessException ex){
            throw new ControllerServiceException(HttpStatus.SC_FORBIDDEN, ex.getMessage());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage(), e);
        }
    }

    private ParsedRequestParameters getParsedRequestParameters(InputStream uploadedInputStream, FormDataContentDisposition fileDetail,
                                                               String apMavenSnippet, InputStream apLibraryInputStream, FormDataContentDisposition apLibraryFileDetail,
                                                               String apLibraryMavenSnippet, String apResourceId, String apLibraryResourceId, String managedLibraryName,
                                                               String plansAttributesAsString, String functionsAttributesAsString, String tokenSelectionCriteriaAsString) {
        AutomationPackageFileSource apFileSource = getApFileSource(uploadedInputStream, fileDetail,
                apMavenSnippet, apResourceId);
        AutomationPackageFileSource apLibrarySource = getLibraryFileSource(apLibraryInputStream, apLibraryFileDetail,
                apLibraryMavenSnippet, apLibraryResourceId, managedLibraryName);

        Map<String, String> plansAttributes = deserializeFormDataParamToMapOfStrings(plansAttributesAsString, PLANS_ATTRIBUTES);
        Map<String, String> functionsAttributes = deserializeFormDataParamToMapOfStrings(functionsAttributesAsString, FUNCTIONS_ATTRIBUTES);
        Map<String, String> tokenSelectionCriteria = deserializeFormDataParamToMapOfStrings(tokenSelectionCriteriaAsString, TOKEN_SELECTION_CRITERIA);
        return new ParsedRequestParameters(apFileSource, apLibrarySource, plansAttributes, functionsAttributes, tokenSelectionCriteria);
    }

    private static class ParsedRequestParameters {
        public final AutomationPackageFileSource apFileSource;
        public final AutomationPackageFileSource apLibrarySource;
        public final Map<String, String> plansAttributes;
        public final Map<String, String> functionsAttributes;
        public final Map<String, String> tokenSelectionCriteria;

        public ParsedRequestParameters(AutomationPackageFileSource apFileSource, AutomationPackageFileSource apLibrarySource, Map<String, String> plansAttributes, Map<String, String> functionsAttributes, Map<String, String> tokenSelectionCriteria) {
            this.apFileSource = apFileSource;
            this.apLibrarySource = apLibrarySource;
            this.plansAttributes = plansAttributes;
            this.functionsAttributes = functionsAttributes;
            this.tokenSelectionCriteria = tokenSelectionCriteria;
        }
    }

    private Map<String, String> deserializeFormDataParamToMapOfStrings(String stringValue, String fieldName) {
        if (stringValue != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return objectMapper.readValue(stringValue, new TypeReference<Map<String, String>>() {
                });
            } catch (JsonProcessingException e) {
                throw new AutomationPackageManagerException("Cannot deserialize " + fieldName + ". Reason: " + e.getMessage());
            }
        }
        return null;
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

    @POST
    @Path("/resources")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Secured(right = "automation-package-write")
    public String createNewAutomationPackageResource(@FormDataParam("resourceType") String resourceType,
                                                     @FormDataParam("file") InputStream uploadedInputStream,
                                                     @FormDataParam("file") FormDataContentDisposition fileDetail,
                                                     @FormDataParam("mavenSnippet") String mavenSnippet,
                                                     @FormDataParam("managedLibraryName") String managedLibraryName){
        try {
            AutomationPackageUpdateParameter automationPackageUpdateParameter = createAutomationPackageUpdateParameterBuilder().build();
            Resource resource = automationPackageManager.createAutomationPackageResource(
                    resourceType,
                    getLibraryFileSource(uploadedInputStream, fileDetail, mavenSnippet, null, null),
                    managedLibraryName,
                    automationPackageUpdateParameter
            );
            auditLog("create",  resource);
            return resource == null ? null : resource.getId().toHexString();
        } catch (AutomationPackageAccessException ex){
            throw new ControllerServiceException(HttpStatus.SC_FORBIDDEN, ex.getMessage());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage(), e);
        }
    }

    @POST
    @Path("/library")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Secured(right = "automation-package-write")
    public AutomationPackageUpdateResult deployAutomationPackageLibrary(@FormDataParam("file") InputStream uploadedInputStream,
                                                     @FormDataParam("file") FormDataContentDisposition fileDetail,
                                                     @FormDataParam("mavenSnippet") String mavenSnippet,
                                                     @FormDataParam("managedLibraryName") String managedLibraryName){
        try {
            AutomationPackageUpdateParameter automationPackageUpdateParameter = createAutomationPackageUpdateParameterBuilder().build();
            //in case of managed library we first determine if we are creating a new one or updating an existing one, otherwise we always create a new library
            boolean managedLibrary = !StringUtils.isBlank(managedLibraryName);
            String resourceType = (managedLibrary) ? ResourceManager.RESOURCE_TYPE_AP_MANAGED_LIBRARY : ResourceManager.RESOURCE_TYPE_AP_LIBRARY;
            Resource updated = null;
            AutomationPackageUpdateStatus status = null;
            if (managedLibrary) {
                Resource existingResource = automationPackageManager.getResourceManager().getResourceByNameAndType(managedLibraryName, resourceType, getObjectPredicate());
                if (existingResource != null) {
                    updated = automationPackageManager.updateAutomationPackageManagedLibrary(existingResource.getId().toHexString(),
                             getLibraryFileSource(uploadedInputStream, fileDetail, mavenSnippet, null, null),
                             managedLibraryName,
                             automationPackageUpdateParameter);
                    status = AutomationPackageUpdateStatus.UPDATED;
                    auditLog("deploy(update)",  updated);
                }
            }
            //If it's not a managed library or if it's a new managed library we use the create method
            if (!managedLibrary || updated == null) {
                updated = automationPackageManager.createAutomationPackageResource(
                        resourceType,
                        getLibraryFileSource(uploadedInputStream, fileDetail, mavenSnippet, null, null),
                        managedLibraryName,
                        automationPackageUpdateParameter
                );
                status = AutomationPackageUpdateStatus.CREATED;
                auditLog("deploy(create)",  updated);
            }
            return new AutomationPackageUpdateResult(status, updated.getId(), new ConflictingAutomationPackages(), Set.of() );
        } catch (AutomationPackageAccessException ex){
            throw new ControllerServiceException(HttpStatus.SC_FORBIDDEN, ex.getMessage());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage(), e);
        }
    }

    @POST
    @Path("/resources/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Secured(right = "automation-package-write")
    public String updateAutomationPackageResource(@PathParam("id") String id,
                                                     @FormDataParam("file") InputStream uploadedInputStream,
                                                     @FormDataParam("file") FormDataContentDisposition fileDetail,
                                                     @FormDataParam("mavenSnippet") String mavenSnippet,
                                                     @FormDataParam("newManagedLibraryName") String newManagedLibraryName){
        try {
            AutomationPackageUpdateParameter automationPackageUpdateParameter = createAutomationPackageUpdateParameterBuilder()
                    .withForceRefreshOfSnapshots(true).build();
            Resource resource = automationPackageManager.updateAutomationPackageManagedLibrary(
                    id,
                    getLibraryFileSource(uploadedInputStream, fileDetail, mavenSnippet, null, null),
                    newManagedLibraryName,
                    automationPackageUpdateParameter
            );
            auditLog("update",  resource);
            return resource == null ? null : resource.getId().toHexString();
        } catch (AutomationPackageAccessException ex){
            throw new ControllerServiceException(HttpStatus.SC_FORBIDDEN, ex.getMessage());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage(), e);
        }
    }

    @POST
    @Path("/{id}/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public RefreshResourceResult refreshAutomationPackage(@PathParam("id") String automationPackageId){
        try {
            AutomationPackage automationPackage = getAutomationPackage(automationPackageId);
            if (!FileResolver.isResource(automationPackage.getAutomationPackageResource())) {
                throw new ControllerServiceException("The Automation Package (" + automationPackageId + ") cannot be refreshed because the resource path of his package file is invalid '"  +
                        automationPackage.getAutomationPackageResource() + "'. Resource paths should be in the format 'resource:<id>'.");
            }
            return refreshResourceAndLinkedPackages(FileResolver.resolveResourceId(automationPackage.getAutomationPackageResource()));
        } catch (AutomationPackageAccessException ex){
            throw new ControllerServiceException(HttpStatus.SC_FORBIDDEN, ex.getMessage());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage(), e);
        }
    }

    @POST
    @Path("/bulk/refresh")
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public AsyncTaskStatus<TableBulkOperationReport> bulkRefresh(TableBulkOperationRequest request) {
        Consumer<String> consumer = this::refreshAutomationPackage;
        return scheduleAsyncTaskWithinSessionContext(h ->
                tableService.performBulkOperation(AutomationPackageEntity.entityName, request, consumer, getSession()));
    }

    @POST
    @Path("/resources/{id}/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public RefreshResourceResult refreshAutomationPackageResource(@PathParam("id") String resourceId){
        try {
            return refreshResourceAndLinkedPackages(resourceId);
        } catch (AutomationPackageAccessException ex){
            throw new ControllerServiceException(HttpStatus.SC_FORBIDDEN, ex.getMessage());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage(), e);
        }
    }

    private RefreshResourceResult refreshResourceAndLinkedPackages(String resourceId) {
        AutomationPackageUpdateParameter automationPackageUpdateParameter = createAutomationPackageUpdateParameterBuilder()
                .withForceRefreshOfSnapshots(true).build();
        RefreshResourceResult refreshResourceResult = automationPackageManager.getAutomationPackageResourceManager().refreshResourceAndLinkedPackages(resourceId, automationPackageUpdateParameter, automationPackageManager);
        if (!refreshResourceResult.isFailed()){
            auditLogForResource("refresh",  resourceId);
        }
        return refreshResourceResult;
    }

    private AutomationPackageUpdateParameterBuilder createAutomationPackageUpdateParameterBuilder() {
        return new AutomationPackageUpdateParameterBuilder()
                .withEnricher(getObjectEnricher())
                .withObjectPredicate(getObjectPredicate())
                .withWriteAccessValidator(getWriteAccessValidator())
                .withActorUser(getUser());
    }

    @POST
    @Path("/resources/bulk/delete")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-delete")
    public AsyncTaskStatus<TableBulkOperationReport> bulkDeleteAutomationPackageResource(TableBulkOperationRequest request) {
        Consumer<String> consumer = resourceId -> {
            try {
                automationPackageManager.getAutomationPackageResourceManager().deleteResource(resourceId, getWriteAccessValidator());
                auditLogForResource("delete",  resourceId);
            } catch (AutomationPackageUnsupportedResourceTypeException e) {
                throw new BulkOperationWarningException(e.getMessage());
            }
        };
        return scheduleAsyncTaskWithinSessionContext(h ->
                tableService.performBulkOperation(EntityManager.resources, request, consumer, getSession()));
    }

    @POST
    @Path("/resources/bulk/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public AsyncTaskStatus<TableBulkOperationReport> bulkRefreshAutomationPackageResource(TableBulkOperationRequest request) {
        Consumer<String> consumer = this::refreshResourceAndLinkedPackages;
        return scheduleAsyncTaskWithinSessionContext(h ->
                tableService.performBulkOperation(EntityManager.resources, request, consumer, getSession()));
    }

    @DELETE
    @Path("/resources/{id}")
    @Secured(right = "automation-package-delete")
    public void deleteAutomationPackageResource(@PathParam("id") String resourceId) {
        try {
            automationPackageManager.getAutomationPackageResourceManager().deleteResource(resourceId, getWriteAccessValidator());
            auditLogForResource("delete",  resourceId);
        } catch (AutomationPackageAccessException ex) {
            throw new ControllerServiceException(HttpStatus.SC_FORBIDDEN, ex.getMessage());
        } catch (AutomationPackageManagerException | AutomationPackageUnsupportedResourceTypeException e) {
            throw new ControllerServiceException(e.getMessage(), e);
        }
    }

    @GET
    @Path("/resources/{id}/automation-packages")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-read")
    public List<AutomationPackage> getLinkedAutomationPackagesForResource(@PathParam("id") String resourceId) {
        try {
            return automationPackageManager.getAutomationPackageResourceManager().findAutomationPackagesByResourceId(resourceId, List.of());
        } catch (AutomationPackageAccessException ex) {
            throw new ControllerServiceException(HttpStatus.SC_FORBIDDEN, ex.getMessage());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage(), e);
        }
    }
}