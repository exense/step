package step.controller.services.entities;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.bson.types.ObjectId;
import step.automation.packages.AutomationPackageEntity;
import step.controller.services.async.AsyncTaskStatus;
import step.core.GlobalContext;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.AbstractTrackedObject;
import step.core.accessors.Accessor;
import step.core.deployment.AbstractStepAsyncServices;
import step.core.deployment.ControllerServiceException;
import step.core.entities.Entity;
import step.framework.server.audit.AuditLogger;
import step.framework.server.security.Secured;
import step.framework.server.tables.service.TableRequest;
import step.framework.server.tables.service.TableResponse;
import step.framework.server.tables.service.TableService;
import step.framework.server.tables.service.TableServiceException;
import step.framework.server.tables.service.bulk.TableBulkOperationReport;
import step.framework.server.tables.service.bulk.TableBulkOperationRequest;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class AbstractEntityServices<T extends AbstractIdentifiableObject> extends AbstractStepAsyncServices {

    public static String CUSTOM_FIELD_LOCKED = "locked";
    private final String entityName;
    private Accessor<T> accessor;
    private TableService tableService;

    public AbstractEntityServices(String entityName) {
        this.entityName = entityName;
    }

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        Entity<T, Accessor<T>> entityType = (Entity<T, Accessor<T>>) context.getEntityManager().getEntityByName(entityName);
        accessor = entityType.getAccessor();
        tableService = context.require(TableService.class);
    }



    @Operation(operationId = "get{Entity}ById", description = "Retrieves an entity by its Id")
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public T get(@PathParam("id") String id) {
        return accessor.get(id);
    }
    
    @Operation(operationId = "find{Entity}sByIds", description = "Returns the list of entities for the provided list of IDs")
    @POST
    @Path("/find/by/ids")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public List<T> findByIds(List<String> ids) {
        return accessor.findByIds(ids).collect(Collectors.toList());
    }

    @Operation(operationId = "find{Entity}NamesByIds", description = "Returns the map of entities IDs to names for the provided list of IDs")
    @POST
    @Path("/find/names/by/ids")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public Map<String, String>  findNamesByIds(List<String> ids) {
        return accessor.findByIds(ids).collect(Collectors.toMap(a -> a.getId().toHexString(), a ->
            (a instanceof AbstractOrganizableObject) ?
                    ((AbstractOrganizableObject) a).getAttribute(AbstractOrganizableObject.NAME) :
                    "unresolved"
        ));
    }

    @Operation(operationId = "find{Entity}sByAttributes", description = "Returns the list of entities matching the provided attributes")
    @POST
    @Path("/find")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public List<T> findManyByAttributes(Map<String, String> attributes) {
        Spliterator<T> manyByAttributes = accessor.findManyByAttributes(attributes);
        return StreamSupport.stream(manyByAttributes, false).collect(Collectors.toList());
    }

    @Operation(operationId = "delete{Entity}", description = "Deletes the entity with the given Id")
    @DELETE
    @Path("/{id}")
    @Secured(right = "{entity}-delete")
    public void delete(@PathParam("id") String id) {
        T entity = getEntity(id);
        assertEntityIsEditableInContext(entity);
        auditLog("delete", entity);
        accessor.remove(new ObjectId(id));
    }

    @Operation(operationId = "clone{Entity}s", description = "Clones the entities according to the provided parameters")
    @POST
    @Path("/bulk/clone")
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-write")
    public AsyncTaskStatus<TableBulkOperationReport> cloneEntities(TableBulkOperationRequest request) {
        return scheduleAsyncTaskWithinSessionContext(h ->
                tableService.performBulkOperation(entityName, request, this::clone, getSession()));
    }

    @Operation(operationId = "save{Entity}", description = "Saves the provided entity")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-write")
    public T save(T entity) {
        trackEntityIfApplicable(entity);
        entity = beforeSave(entity);
        T result = accessor.save(entity);
        auditLog("save", result);
        return result;
    }

    protected void auditLog(String operation, T entity) {
        auditLog(operation, entity, null);
    }

    protected void auditLog(String operation, T entity, Map<String, String> moreAttributes) {
        if (entity == null || !AuditLogger.isEntityModificationsLoggingEnabled()) {
            return;
        }
        String entityName = null;
        if (entity instanceof AbstractOrganizableObject) {
            // this should always be the case
            AbstractOrganizableObject a = (AbstractOrganizableObject) entity;
            entityName = a.getAttribute(AbstractOrganizableObject.NAME);
        }
        Map<String, String> attributes = new LinkedHashMap<>(Objects.requireNonNullElse(getObjectEnricher().getAdditionalAttributes(), Map.of()));
        Optional.ofNullable(moreAttributes).ifPresent(attributes::putAll);
        AuditLogger.logEntityModification(getSession(), operation, this.entityName, entity.getId().toHexString(), entityName, attributes);
    }

    private void trackEntityIfApplicable(T entity) {
        if (AbstractTrackedObject.class.isAssignableFrom(entity.getClass())) {
            AbstractTrackedObject newTrackedEntity = (AbstractTrackedObject) entity;
            ObjectId sourceId = entity.getId();
            T sourceEntity = (sourceId != null) ? accessor.get(sourceId) : null;
            String username = getSession().getUser().getUsername();
            Date lastModificationDate = new Date();
            if (sourceEntity == null) {
                newTrackedEntity.setCreationDate(lastModificationDate);
                newTrackedEntity.setCreationUser(username);
            }
            newTrackedEntity.setLastModificationDate(lastModificationDate);
            newTrackedEntity.setLastModificationUser(username);
        }
    }

    @Operation(operationId = "clone{Entity}", description = "Clones the entity with the given Id")
    @GET
    @Path("/{id}/clone")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-write")
    public T clone(@PathParam("id") String id) {
        T entity = getEntity(id);
        T clonedEntity = cloneEntity(entity);

        if (clonedEntity instanceof AbstractOrganizableObject) {
            AbstractOrganizableObject organizableObject = (AbstractOrganizableObject) clonedEntity;
            // Append _Copy to new plan name
            String name = organizableObject.getAttribute(AbstractOrganizableObject.NAME);
            String newName = name + "_Copy";
            organizableObject.addAttribute(AbstractOrganizableObject.NAME, newName);
            //Remove flags
            Map<String, Object> customFields = organizableObject.getCustomFields();
            if (customFields != null) {
                customFields.remove(CUSTOM_FIELD_LOCKED);
                customFields.remove(AutomationPackageEntity.AUTOMATION_PACKAGE_ID);
            }
        }
        // Save the cloned entity
        assertEntityIsEditableInContext(clonedEntity);
        auditLog("clone", clonedEntity);
        save(clonedEntity);
        return clonedEntity;
    }

    protected T cloneEntity(T entity) {
        entity.setId(new ObjectId());
        return entity;
    }

    protected T beforeSave(T entity) {
        return entity;
    }

    @Operation(operationId = "delete{Entity}s", description = "Deletes the entities according to the provided parameters")
    @POST
    @Path("/bulk/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-delete")
    public AsyncTaskStatus<TableBulkOperationReport> bulkDelete(TableBulkOperationRequest request) {
        Consumer<String> consumer = t -> {
            try {
                delete(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        return scheduleAsyncTaskWithinSessionContext(h ->
                tableService.performBulkOperation(entityName, request, consumer, getSession()));
    }

    @Operation(operationId = "get{Entity}Table", description = "Get the table view according to provided request")
    @POST
    @Path("/table")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public TableResponse<T> request(TableRequest request) throws TableServiceException {
        return tableService.request(entityName, request, getSession());
    }

    @Operation(operationId = "get{Entity}Versions", description = "Retrieves the versions of the entity with the given id")
    @GET
    @Path("/{id}/versions")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public List<History> getVersions(@PathParam("id") String id) {
        return accessor.getHistory(new ObjectId(id), 0, 1000)
                .map(v->new History(v.getId().toHexString(), v.getUpdateTime()))
                .collect(Collectors.toList());
    }

    public static class History {
        public String id;
        public long updateTime;

        public History(String id, long updateTime) {
            this.id = id;
            this.updateTime = updateTime;
        }
    }

    @Operation(operationId = "restore{Entity}Version", description = "Restore a version of this entity")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-write")
    @Path("{id}/restore/{versionId}")
    public T restoreVersion(@PathParam("id") String id, @PathParam("versionId") String versionId) {
        assertEntityIsEditableInContext(getEntity(id));
        T result = accessor.restoreVersion(new ObjectId(id), new ObjectId(versionId));
        auditLog("restoreVersion", result, Map.of("restoredVersion", versionId));
        return result;
    }

    @Operation(operationId = "is{Entity}Locked", description = "Get entity locking state")
    @GET
    @Path("/{id}/locked")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public boolean isLocked(@PathParam("id") String id) {
        T t = getEntity(id);
        Boolean locked = t.getCustomField(CUSTOM_FIELD_LOCKED, Boolean.class);
        return (locked != null && locked);
    }

    @Operation(operationId = "lock{Entity}", description = "Lock this entity")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-write")
    @Path("{id}/locked")
    public void setLocked(@PathParam("id") String id, Boolean locked) {
        if (locked == null) {
            locked = false;
        }
        T t = getEntity(id);
        //when unlocking we need to set the new state before calling assertEntityIsAcceptableInContext
        if (!locked) {
            t.addCustomField(CUSTOM_FIELD_LOCKED, locked);
        }
        assertEntityIsEditableInContext(t);
        t.addCustomField(CUSTOM_FIELD_LOCKED, locked);
        auditLog("set-lock", t, Map.of("locked", Objects.toString(locked)));
        accessor.save(t);
    }

    protected T getEntity(String id) {
        T t = accessor.get(id);
        if (t == null) {
            throw new ControllerServiceException("The entity with id '" + id + "' does not exists.");
        } else {
            return t;
        }
    }
}
