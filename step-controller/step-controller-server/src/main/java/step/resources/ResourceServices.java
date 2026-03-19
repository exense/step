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
package step.resources;

import ch.exense.commons.io.FileHelper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import step.controller.services.async.AsyncTaskStatus;
import step.core.GlobalContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.deployment.AbstractStepAsyncServices;
import step.core.deployment.ControllerServiceException;
import step.core.entities.EntityConstants;
import step.core.objectenricher.ObjectEnricher;
import step.framework.server.audit.AuditLogger;
import step.framework.server.security.Secured;
import step.framework.server.tables.service.TableService;
import step.framework.server.tables.service.bulk.TableBulkOperationReport;
import step.framework.server.tables.service.bulk.TableBulkOperationRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Path("/resources")
@Tag(name = "Resources")
public class ResourceServices extends AbstractStepAsyncServices {

    private static final String RESOURCE_RIGHT_NAME = "resource";

    protected ResourceManager resourceManager;
    private TableService tableService;

    @jakarta.ws.rs.core.Context
    ServletContext context;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext globalContext = getContext();
        resourceManager = globalContext.getResourceManager();
        tableService = globalContext.require(TableService.class);
    }

    private void auditLog(String operation, Resource resource) {
        if (resource == null || !AuditLogger.isEntityModificationsLoggingEnabled()) {
            return;
        }
        String entityName = resource.getAttribute(AbstractOrganizableObject.NAME);
        Map<String, String> attributes = new LinkedHashMap<>(Objects.requireNonNullElse(getObjectEnricher().getAdditionalAttributes(), Map.of()));
        attributes.put("resourceType", resource.getResourceType());
        AuditLogger.logEntityModification(getSession(), operation, "resources", resource.getId().toHexString(), entityName, attributes);
    }

    private void checkResourceTypeRight(String resourceType, String right) {
        checkRightIfDefined(RESOURCE_RIGHT_NAME + RIGHT_SEPARATOR + resourceType +  RIGHT_SEPARATOR + right);
    }

    private void checkResourceTypeRight(Resource resource, String right) {
        //We have different services where we do not explicitly check if the type is set, for this reason that if type is not set we grant access
        String resourceType = resource.getResourceType();
        if (resourceType != null && !resourceType.isEmpty()) {
            checkResourceTypeRight(resourceType, right);
        }
    }

    @POST
    @Path("/content")
    @Secured(right = RESOURCE_RIGHT_NAME + RIGHT_SEPARATOR + WRITE_RIGHT)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public ResourceUploadResponse createResource(@FormDataParam("file") InputStream uploadedInputStream,
                                                 @FormDataParam("file") FormDataContentDisposition fileDetail,
                                                 @QueryParam("type") String resourceType,
                                                 @QueryParam("directory") Boolean isDirectory,
                                                 @QueryParam("trackingAttribute") String trackingAttribute,
                                                 @QueryParam("origin") String origin,
                                                 @QueryParam("originTimestamp") Long originTimestamp) throws IOException {
        ObjectEnricher objectEnricher = getObjectEnricher();

        if (uploadedInputStream == null || fileDetail == null)
            throw new RuntimeException("Invalid arguments");
        if (resourceType == null || resourceType.isEmpty())
            throw new RuntimeException("Missing resource type query parameter 'type'");

        checkResourceTypeRight(resourceType, WRITE_RIGHT);
        try {
            Resource resource = resourceManager.createTrackedResource(
                resourceType, isDirectory, uploadedInputStream, fileDetail.getFileName(), objectEnricher,
                trackingAttribute, getSession().getUser().getUsername(),
                origin == null ? new UploadedResourceOrigin().toStringRepresentation() : origin,
                originTimestamp
            );
            auditLog("create", resource);
            return new ResourceUploadResponse(resource, null);
        } catch (InvalidResourceFormatException e) {
            throw uploadFileNotAnArchive();
        }
    }

    private ControllerServiceException uploadFileNotAnArchive() {
        return new ControllerServiceException("The uploaded file is not an archive. Please upload a zip of the folder.");
    }

    @POST
    @Secured(right = RESOURCE_RIGHT_NAME + RIGHT_SEPARATOR + WRITE_RIGHT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Resource saveResource(Resource resource) throws IOException {
        checkResourceTypeRight(resource, WRITE_RIGHT);
        auditLog("save", resource);
        return resourceManager.saveResource(resource);
    }

    @POST
    @Path("/{id}/content")
    @Secured(right = RESOURCE_RIGHT_NAME + RIGHT_SEPARATOR + WRITE_RIGHT)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public ResourceUploadResponse saveResourceContent(@PathParam("id") String resourceId, @FormDataParam("file") InputStream uploadedInputStream,
                                                      @FormDataParam("file") FormDataContentDisposition fileDetail) throws Exception {
        if (uploadedInputStream == null || fileDetail == null)
            throw new RuntimeException("Invalid arguments");

        checkResourceTypeRight(getResource(resourceId), WRITE_RIGHT);
        try {
            Resource resource = resourceManager.saveResourceContent(resourceId, uploadedInputStream, fileDetail.getFileName(), null, getSession().getUser().getUsername());
            auditLog("save-content", resource);
            return new ResourceUploadResponse(resource, null);
        } catch (InvalidResourceFormatException e) {
            throw uploadFileNotAnArchive();
        }
    }

    @GET
    @Secured
    @Path("/{id}")
    @Secured(right = RESOURCE_RIGHT_NAME + RIGHT_SEPARATOR + READ_RIGHT)
    @Produces(MediaType.APPLICATION_JSON)
    public Resource getResource(@PathParam("id") String resourceId) throws IOException {
        try {
            Resource resource = resourceManager.getResource(resourceId);
            checkResourceTypeRight(resource, READ_RIGHT);
            return resource;
        } catch (ResourceMissingException e) {
            throw new ControllerServiceException(404, e.getMessage());
        }
    }

    @GET
    @Path("/{id}/content")
    @Secured(right = RESOURCE_RIGHT_NAME + RIGHT_SEPARATOR + READ_RIGHT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResourceContent(@PathParam("id") String resourceId, @QueryParam("inline") boolean inline) throws IOException {
        ResourceRevisionContent resourceContent = resourceManager.getResourceContent(resourceId);
        checkResourceTypeRight(resourceContent.getResource(), READ_RIGHT);
        return getResponseForResourceRevisionContent(resourceContent, inline);
    }

    @DELETE
    @Secured
    @Path("/{id}")
    @Secured(right = RESOURCE_RIGHT_NAME + RIGHT_SEPARATOR + DELETE_RIGHT)
    public void deleteResource(@PathParam("id") String resourceId) {
        Resource resource = resourceManager.getResource(resourceId);
        checkResourceTypeRight(resource, DELETE_RIGHT);
        assertEntityIsEditableInContext(resource);
        auditLog("delete", resource);
        resourceManager.deleteResource(resourceId);
    }

    @DELETE
    @Secured
    @Path("/{id}/revisions")
    @Secured(right = RESOURCE_RIGHT_NAME + RIGHT_SEPARATOR + DELETE_RIGHT)
    public void deleteResourceRevisions(@PathParam("id") String resourceId) {
        Resource resource = resourceManager.getResource(resourceId);
        checkResourceTypeRight(resource, DELETE_RIGHT);
        assertEntityIsEditableInContext(resource);
        auditLog("delete-revisions", resource);
        resourceManager.deleteResourceRevisionContent(resourceId);
    }

    @POST
    @Path("/bulk/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "resource-bulk-delete")
    public AsyncTaskStatus<TableBulkOperationReport> bulkDelete(TableBulkOperationRequest request) {
        Consumer<String> consumer = t -> {
            try {
                deleteResource(t);
            } catch (Throwable e) {
                if (e instanceof RuntimeException) {
                    throw e;
                } else {
                    throw new RuntimeException(e);
                }
            }
        };
        return scheduleAsyncTaskWithinSessionContext(h ->
            tableService.performBulkOperation(EntityConstants.resources, request, consumer, getSession()));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/revision/{id}/content")
    @Secured(right = RESOURCE_RIGHT_NAME + RIGHT_SEPARATOR + READ_RIGHT)
    public Response getResourceRevisionContent(@PathParam("id") String resourceRevisionId, @QueryParam("inline") boolean inline) throws IOException {
        ResourceRevisionContentImpl resourceContent = resourceManager.getResourceRevisionContent(resourceRevisionId);
        checkResourceTypeRight(resourceContent.getResource(), READ_RIGHT);
        return getResponseForResourceRevisionContent(resourceContent, inline);
    }

    @POST
    @Path("/find")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = RESOURCE_RIGHT_NAME + RIGHT_SEPARATOR + READ_RIGHT)
    public List<Resource> findManyByCriteria(Map<String, String> criteria) {
        return resourceManager.findManyByCriteria(criteria).stream().filter(r -> {
            try {
                checkResourceTypeRight(r, READ_RIGHT);
                return true;
            } catch (Exception e) {
                //we filter out the resources for which the user has no access
                return false;
            }
        }).collect(Collectors.toList());
    }

    protected Response getResponseForResourceRevisionContent(ResourceRevisionContent resourceContent, boolean inline) {
        StreamingOutput fileStream = new StreamingOutput() {
            @Override
            public void write(java.io.OutputStream output) throws IOException {
                FileHelper.copy(resourceContent.getResourceStream(), output, 2048);
                resourceContent.close();
            }
        };

        String resourceName = resourceContent.getResourceName();
        String mimeType = context.getMimeType(resourceName);
        if (mimeType == null) {
            if (resourceName.endsWith(".log")) {
                mimeType = "text/plain; charset=utf-8";
            } else {
                mimeType = "application/octet-stream";
            }
        }

        String contentDisposition;
        if (inline) {
            contentDisposition = "inline";
        } else {
            contentDisposition = "attachment";
        }

        String headerValue = String.format(contentDisposition + "; filename=\"%s\"", resourceName);

        return Response.ok(fileStream, mimeType)
            .header("content-disposition", headerValue).build();
    }
}
