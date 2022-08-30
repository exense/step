package step.controller.services.entities;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.bson.types.ObjectId;
import step.controller.services.async.AsyncTaskManager;
import step.controller.services.async.AsyncTaskStatus;
import step.controller.services.bulk.BulkOperationManager;
import step.controller.services.bulk.BulkOperationParameters;
import step.core.GlobalContext;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.collections.Collection;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.ControllerServiceException;
import step.core.entities.Entity;
import step.core.objectenricher.ObjectFilter;
import step.framework.server.security.Secured;

import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class AbstractEntityServices<T extends AbstractIdentifiableObject> extends AbstractStepServices {

    private final String entityName;
    private Entity<T, Accessor<T>> entityType;
    private BulkOperationManager bulkOperationManager;
    private Accessor<T> accessor;
    private Collection<T> collection;

    public AbstractEntityServices(String entityName) {
        this.entityName = entityName;
    }

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        entityType = (Entity<T, Accessor<T>>) context.getEntityManager().getEntityByName(entityName);
        accessor = entityType.getAccessor();
        collection = accessor.getCollectionDriver();
        bulkOperationManager = new BulkOperationManager(context.require(AsyncTaskManager.class));
    }

    @Operation(operationId = "get{Entity}ById")
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "read")
    public T get(@PathParam("id") String id) {
        return accessor.get(id);
    }

    @Operation(operationId = "find{Entity}sByAttributes")
    @POST
    @Path("/find")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "read")
    public List<T> findManyByAttributes(Map<String, String> attributes) {
        Spliterator<T> manyByAttributes = accessor.findManyByAttributes(attributes);
        return StreamSupport.stream(manyByAttributes, false).collect(Collectors.toList());
    }

    @Operation(operationId = "delete{Entity}")
    @DELETE
    @Path("/{id}")
    @Secured(right = "delete")
    public void delete(@PathParam("id") String id) throws Exception {
        accessor.remove(new ObjectId(id));
    }

    @Operation(operationId = "clone{Entity}s")
    @POST
    @Path("/bulk/clone")
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "write")
    public AsyncTaskStatus<Void> cloneEntities(BulkOperationParameters parameters) {
        ObjectFilter contextObjectFilter = getObjectFilter();
        Collection<T> collection = entityType.getAccessor().getCollectionDriver();
        return bulkOperationManager.performBulkOperation(parameters, this::clone,
                filter -> collection.find(filter, null, null, null, 0)
                        .forEach(plan -> clone(plan.getId().toString())), contextObjectFilter);
    }

    @Operation(operationId = "save{Entity}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "write")
    public T save(T entity) {
        entity = beforeSave(entity);
        return accessor.save(entity);
    }

    @Operation(operationId = "clone{Entity}")
    @GET
    @Path("/{id}/clone")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "write")
    public T clone(@PathParam("id") String id) {
        T entity = get(id);
        if (entity == null) {
            throw new ControllerServiceException("The entity with Id " + id + " doesn't exist");
        }
        T clonedEntity = cloneEntity(entity);

        if (clonedEntity instanceof AbstractOrganizableObject) {
            AbstractOrganizableObject organizableObject = (AbstractOrganizableObject) clonedEntity;
            // Append _Copy to new plan name
            String name = organizableObject.getAttribute(AbstractOrganizableObject.NAME);
            String newName = name + "_Copy";
            organizableObject.addAttribute(AbstractOrganizableObject.NAME, newName);
        }
        // Save the cloned plan
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

    @Operation(operationId = "delete{Entity}s")
    @POST
    @Path("/bulk/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "delete")
    public AsyncTaskStatus<Void> bulkDelete(BulkOperationParameters parameters) {
        ObjectFilter contextObjectFilter = getObjectFilter();
        return bulkOperationManager.performBulkOperation(parameters, t -> {
            try {
                delete(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, collection::remove, contextObjectFilter);
    }
}
