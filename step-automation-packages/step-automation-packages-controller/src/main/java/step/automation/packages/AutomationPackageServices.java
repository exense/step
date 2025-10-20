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
import com.google.api.client.http.HttpStatusCodes;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
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
import step.framework.server.security.Secured;
import step.framework.server.tables.service.TableService;
import step.framework.server.tables.service.bulk.TableBulkOperationReport;
import step.framework.server.tables.service.bulk.TableBulkOperationRequest;
import step.resources.Resource;

import java.io.InputStream;
import java.util.List;
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
            automationPackageManager.removeAutomationPackage(new ObjectId(id), getSession().getUser().getUsername(),getObjectPredicate(), getWriteAccessPredicate());
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
     * @param allowUpdateOfOtherPackages if other AP would be modified (because they use the same snapshot artefacts that has a new version) this flag must be true to allow the update
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
    @Produces(MediaType.TEXT_PLAIN)
    @Secured(right = "automation-package-write")
    public String createAutomationPackage(@FormDataParam("version") String apVersion,
                                          @FormDataParam("activationExpr") String activationExpression,
                                          @FormDataParam("allowUpdateOfOtherPackages") boolean allowUpdateOfOtherPackages,
                                          @FormDataParam("file") InputStream automationPackageInputStream,
                                          @FormDataParam("file") FormDataContentDisposition fileDetail,
                                          @FormDataParam("apMavenSnippet") String apMavenSnippet,
                                          @FormDataParam("apLibrary") InputStream apLibraryInputStream,
                                          @FormDataParam("apLibrary") FormDataContentDisposition apLibraryFileDetail,
                                          @FormDataParam("apLibraryMavenSnippet") String apLibraryMavenSnippet,
                                          @FormDataParam("apResourceId") String apResourceId,
                                          @FormDataParam("apLibraryResourceId") String apLibraryResourceId,
                                          @FormDataParam(PLANS_ATTRIBUTES) String plansAttributesAsString,
                                          @FormDataParam(FUNCTIONS_ATTRIBUTES) String functionsAttributesAsString,
                                          @FormDataParam(TOKEN_SELECTION_CRITERIA) String tokenSelectionCriteriaAsString,
                                          @FormDataParam(EXECUTE_FUNCTIONS_LOCALLY) boolean executeFunctionsLocally) {
        try {
            ParsedRequestParameters parsedRequestParameters = getParsedRequestParamteres(automationPackageInputStream, fileDetail, apMavenSnippet, apLibraryInputStream, apLibraryFileDetail, apLibraryMavenSnippet, apResourceId, apLibraryResourceId, plansAttributesAsString, functionsAttributesAsString, tokenSelectionCriteriaAsString);

            AutomationPackageUpdateParameter parameters = getAutomationPackageUpdateParameterBuilder()
                    .withAllowCreate(true).withAllowUpdate(false).withAsync(false)
                    .withAllowUpdateOfOtherPackages(allowUpdateOfOtherPackages).withCheckForSameOrigin(true)
                    .withApSource(parsedRequestParameters.apFileSource).withApLibrarySource(parsedRequestParameters.apLibrarySource)
                    .withAutomationPackageVersion(apVersion).withActivationExpression(activationExpression)
                    .withPlansAttributes(parsedRequestParameters.plansAttributes).withFunctionsAttributes(parsedRequestParameters.functionsAttributes)
                    .withTokenSelectionCriteria(parsedRequestParameters.tokenSelectionCriteria).withExecuteFunctionLocally(executeFunctionsLocally)
                    .build();
            ObjectId id = automationPackageManager.createOrUpdateAutomationPackage(parameters).getId();
            return id == null ? null : id.toString();
        } catch (AutomationPackageCollisionException e){
            throw new ControllerServiceException(HttpStatusCodes.STATUS_CODE_CONFLICT, COLLISION_ERROR_NAME, e.getMessage());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage(), e);
        }
    }

    private AutomationPackageUpdateParameterBuilder getAutomationPackageUpdateParameterBuilder() {
        return new AutomationPackageUpdateParameterBuilder()
                .withEnricher(getObjectEnricher())
                .withObjectPredicate(getObjectPredicate())
                .withWriteAccessPredicate(getWriteAccessPredicate())
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
                                                 @FormDataParam("apLibrary") InputStream apLibraryInputStream,
                                                 @FormDataParam("apLibrary") FormDataContentDisposition apLibraryFileDetail,
                                                 @FormDataParam("executionParams") FormDataBodyPart executionParamsBodyPart,
                                                 @FormDataParam("apLibraryMavenSnippet") String apLibraryMavenSnippet,
                                                 @FormDataParam("apResourceId") String apResourceId,
                                                 @FormDataParam("apLibraryResourceId") String apLibraryResourceId) {
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
            AutomationPackageFileSource apLibrarySource = getFileSource(apLibraryInputStream, apLibraryFileDetail, apLibraryMavenSnippet, "Invalid maven snippet for automation package library: ", apLibraryResourceId);
            AutomationPackageFileSource apSource = getFileSource(automationPackageInputStream, fileDetail, apMavenSnippet, "Invalid maven snippet for ap: ", apResourceId);

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
                                                                 @FormDataParam("version") String apVersion,
                                                                 @FormDataParam("activationExpr") String activationExpression,
                                                                 @FormDataParam("allowUpdateOfOtherPackages") boolean allowUpdateOfOtherPackages,
                                                                 @FormDataParam("file") InputStream uploadedInputStream,
                                                                 @FormDataParam("file") FormDataContentDisposition fileDetail,
                                                                 @FormDataParam("apMavenSnippet") String apMavenSnippet,
                                                                 @FormDataParam("apLibrary") InputStream apLibraryInputStream,
                                                                 @FormDataParam("apLibrary") FormDataContentDisposition apLibraryFileDetail,
                                                                 @FormDataParam("apLibraryMavenSnippet") String apLibraryMavenSnippet,
                                                                 @FormDataParam("apResourceId") String apResourceId,
                                                                 @FormDataParam("apLibraryResourceId") String apLibraryResourceId,
                                                                 @FormDataParam(PLANS_ATTRIBUTES) String plansAttributesAsString,
                                                                 @FormDataParam(FUNCTIONS_ATTRIBUTES) String functionsAttributesAsString,
                                                                 @FormDataParam(TOKEN_SELECTION_CRITERIA) String tokenSelectionCriteriaAsString,
                                                                 @FormDataParam(EXECUTE_FUNCTIONS_LOCALLY) boolean executeFunctionsLocally) {
        try {
            ParsedRequestParameters parsedRequestParameters = getParsedRequestParamteres(uploadedInputStream, fileDetail, apMavenSnippet, apLibraryInputStream, apLibraryFileDetail, apLibraryMavenSnippet, apResourceId, apLibraryResourceId, plansAttributesAsString, functionsAttributesAsString, tokenSelectionCriteriaAsString);

            AutomationPackageUpdateParameter updateParameters = getAutomationPackageUpdateParameterBuilder().withAllowCreate(false).withExplicitOldId(new ObjectId(id))
                    .withApSource(parsedRequestParameters.apFileSource).withApLibrarySource(parsedRequestParameters.apLibrarySource)
                    .withAutomationPackageVersion(apVersion).withActivationExpression(activationExpression)
                    .withAsync(async).withAllowUpdateOfOtherPackages(allowUpdateOfOtherPackages)
                    .withPlansAttributes(parsedRequestParameters.plansAttributes).withFunctionsAttributes(parsedRequestParameters.functionsAttributes)
                    .withTokenSelectionCriteria(parsedRequestParameters.tokenSelectionCriteria).withExecuteFunctionLocally(executeFunctionsLocally)
                    .build();
            return automationPackageManager.createOrUpdateAutomationPackage(updateParameters);
        } catch (AutomationPackageAccessException ex) {
            throw new ControllerServiceException(HttpStatus.SC_FORBIDDEN, ex.getMessage());
        } catch (AutomationPackageCollisionException e) {
            throw new ControllerServiceException(HttpStatusCodes.STATUS_CODE_CONFLICT, COLLISION_ERROR_NAME, e.getMessage());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    private AutomationPackageFileSource getFileSource(InputStream uploadedInputStream, FormDataContentDisposition fileDetail, String apMavenSnippet, String invalidSnippetErrorText, String resourceId) {
        AutomationPackageFileSource automationPackageFileSource = null;
        try {
            automationPackageFileSource = AutomationPackageFileSource.empty();
            if (uploadedInputStream != null) {
                automationPackageFileSource.addInputStream(uploadedInputStream, fileDetail == null ? null : fileDetail.getFileName());
            }
            if (apMavenSnippet != null) {
                automationPackageFileSource.addMavenIdentifier(getMavenArtifactIdentifierFromXml(apMavenSnippet));
            }
            if(resourceId != null){
                automationPackageFileSource.addResourceId(resourceId);
            }
        } catch (JsonProcessingException e) {
            throw new ControllerServiceException(invalidSnippetErrorText + e.getMessage());
        }
        return automationPackageFileSource;
    }

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-write")
    public Response createOrUpdateAutomationPackage(@FormDataParam("async") boolean async,
                                                    @FormDataParam("version") String apVersion,
                                                    @FormDataParam("allowUpdateOfOtherPackages") boolean allowUpdateOfOtherPackages,
                                                    @FormDataParam("activationExpr") String activationExpression,
                                                    @FormDataParam("file") InputStream uploadedInputStream,
                                                    @FormDataParam("file") FormDataContentDisposition fileDetail,
                                                    @FormDataParam("apMavenSnippet") String apMavenSnippet,
                                                    @FormDataParam("apLibrary") InputStream apLibraryInputStream,
                                                    @FormDataParam("apLibrary") FormDataContentDisposition apLibraryFileDetail,
                                                    @FormDataParam("apLibraryMavenSnippet") String apLibraryMavenSnippet,
                                                    @FormDataParam("apResourceId") String apResourceId,
                                                    @FormDataParam("apLibraryResourceId") String apLibraryResourceId,
                                                    @FormDataParam(PLANS_ATTRIBUTES) String plansAttributesAsString,
                                                    @FormDataParam(FUNCTIONS_ATTRIBUTES) String functionsAttributesAsString,
                                                    @FormDataParam(TOKEN_SELECTION_CRITERIA) String tokenSelectionCriteriaAsString,
                                                    @FormDataParam(EXECUTE_FUNCTIONS_LOCALLY) boolean executeFunctionsLocally) {
        try {
            ParsedRequestParameters parsedRequestParameters = getParsedRequestParamteres(uploadedInputStream, fileDetail, apMavenSnippet, apLibraryInputStream,
                    apLibraryFileDetail, apLibraryMavenSnippet, apResourceId, apLibraryResourceId, plansAttributesAsString, functionsAttributesAsString, tokenSelectionCriteriaAsString);

            AutomationPackageUpdateParameter updateParameters = getAutomationPackageUpdateParameterBuilder()
                    .withApSource(parsedRequestParameters.apFileSource).withApLibrarySource(parsedRequestParameters.apLibrarySource)
                    .withAutomationPackageVersion(apVersion).withActivationExpression(activationExpression)
                    .withAsync(async).withAllowUpdateOfOtherPackages(allowUpdateOfOtherPackages)
                    .withPlansAttributes(parsedRequestParameters.plansAttributes).withFunctionsAttributes(parsedRequestParameters.functionsAttributes)
                    .withTokenSelectionCriteria(parsedRequestParameters.tokenSelectionCriteria).withExecuteFunctionLocally(executeFunctionsLocally)
                    .build();
            AutomationPackageUpdateResult result = automationPackageManager.createOrUpdateAutomationPackage(updateParameters);
            Response.ResponseBuilder responseBuilder;
            if (result.getStatus() == AutomationPackageUpdateStatus.CREATED) {
                responseBuilder = Response.status(Response.Status.CREATED);
            } else {
                responseBuilder = Response.status(Response.Status.OK);
            }
            return responseBuilder.entity(result).build();
        } catch (AutomationPackageAccessException ex){
            throw new ControllerServiceException(HttpStatus.SC_FORBIDDEN, ex.getMessage());
        } catch (AutomationPackageCollisionException e){
            ControllerServiceException ex = new ControllerServiceException(HttpStatusCodes.STATUS_CODE_CONFLICT, COLLISION_ERROR_NAME, e.getMessage());
            // to avoid stack trace in ErrorFilter
            ex.setTechnicalError(false);
            throw ex;
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage(), e);
        }
    }

    private ParsedRequestParameters getParsedRequestParamteres(InputStream uploadedInputStream, FormDataContentDisposition fileDetail, String apMavenSnippet, InputStream apLibraryInputStream, FormDataContentDisposition apLibraryFileDetail, String apLibraryMavenSnippet, String apResourceId, String apLibraryResourceId, String plansAttributesAsString, String functionsAttributesAsString, String tokenSelectionCriteriaAsString) {
        AutomationPackageFileSource apFileSource = getFileSource(
                uploadedInputStream, fileDetail,
                apMavenSnippet, "Invalid maven snippet for automation package: ",
                apResourceId
        );
        AutomationPackageFileSource apLibrarySource = getFileSource(
                apLibraryInputStream, apLibraryFileDetail,
                apLibraryMavenSnippet, "Invalid maven snippet for automation package library: ",
                apLibraryResourceId
        );

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
    public String createNewAutomationPackageResource(String resourceType,
                                                     @FormDataParam("file") InputStream uploadedInputStream,
                                                     @FormDataParam("file") FormDataContentDisposition fileDetail,
                                                     @FormDataParam("mavenSnippet") String mavenSnippet){
        try {
            AutomationPackageUpdateParameter automationPackageUpdateParameter = getAutomationPackageUpdateParameter();
            Resource resource = automationPackageManager.createAutomationPackageResource(
                    resourceType,
                    getFileSource(uploadedInputStream, fileDetail, mavenSnippet, "Invalid maven snippet", null),
                    automationPackageUpdateParameter
            );
            return resource == null ? null : resource.getId().toHexString();
        } catch (AutomationPackageAccessException ex){
            throw new ControllerServiceException(HttpStatus.SC_FORBIDDEN, ex.getMessage());
        } catch (AutomationPackageManagerException e) {
            throw new ControllerServiceException(e.getMessage(), e);
        }
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
        AutomationPackageUpdateParameter automationPackageUpdateParameter = getAutomationPackageUpdateParameter();
        return automationPackageManager.getAutomationPackageResourceManager().refreshResourceAndLinkedPackages(resourceId, automationPackageUpdateParameter, automationPackageManager);
    }

    private AutomationPackageUpdateParameter getAutomationPackageUpdateParameter() {
        return new AutomationPackageUpdateParameterBuilder()
                .withEnricher(getObjectEnricher())
                .withObjectPredicate(getObjectPredicate())
                .withWriteAccessPredicate(getWriteAccessPredicate())
                .withActorUser(getUser()).build();
    }

    @POST
    @Path("/resources/bulk/delete")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "automation-package-delete")
    public AsyncTaskStatus<TableBulkOperationReport> bulkDeleteAutomationPackageResource(TableBulkOperationRequest request) {
        Consumer<String> consumer = resourceId -> automationPackageManager.getAutomationPackageResourceManager().deleteResource(resourceId, getWriteAccessPredicate());
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
            automationPackageManager.getAutomationPackageResourceManager().deleteResource(resourceId, getWriteAccessPredicate());
        } catch (AutomationPackageAccessException ex) {
            throw new ControllerServiceException(HttpStatus.SC_FORBIDDEN, ex.getMessage());
        } catch (AutomationPackageManagerException e) {
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