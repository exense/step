package step.plugins.services;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.Accessor;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.deployment.AbstractStepServices;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.framework.server.security.Secured;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
@Path("entities")
@Tag(name = "Entities")
public class EntityServices extends AbstractStepServices {

    protected EntityManager entityManager;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        entityManager = getContext().getEntityManager();
    }

    @PreDestroy
    public void destroy() {
    }

    @GET
    @Path("/{type}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured()
    public Response getEntity(@PathParam("type") String type, @PathParam("id") String id) {
        Entity<?, ?> entityType = entityManager.getEntityByName(type);
        AbstractIdentifiableObject entity = entityType.getAccessor().get(id);
        return Response.status(200).entity(entity).build();
    }

    @POST
    @Path("/{type}/find")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured()
    public Response findEntity(@PathParam("type") String type, Map<String, String> attributes) {
        Entity<?, ?> entityType = entityManager.getEntityByName(type);
        Spliterator<?> manyByAttributes = entityType.getAccessor().findManyByAttributes(attributes);

        List<?> collect = StreamSupport.stream(manyByAttributes, false).collect(Collectors.toList());
        Class<?> entityClass = entityType.getEntityClass();
        ParameterizedType parameterizedGenericType = new ParameterizedType() {
            public Type[] getActualTypeArguments() {
                return new Type[]{entityClass};
            }

            public Type getRawType() {
                return List.class;
            }

            public Type getOwnerType() {
                return List.class;
            }
        };

        GenericType<List<?>> genericType = new GenericType<List<?>>(
                parameterizedGenericType) {
        };

        GenericEntity<List<?>> genericEntity = new GenericEntity<>(collect, genericType.getType());
        return Response.status(200).entity(genericEntity).build();
    }

    @DELETE
    @Path("/{type}/{id}")
    @Secured()
    public void deleteEntity(@PathParam("type") String type, @PathParam("id") String id) {
        Entity<?, ?> entityType = entityManager.getEntityByName(type);
        entityType.getAccessor().remove(new ObjectId(id));
    }

    @POST
    @Path("/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured()
    public Response saveEntity(@PathParam("type") String type, Object entity) {
        Entity<?, ?> entityType = entityManager.getEntityByName(type);
        Class<? extends AbstractIdentifiableObject> entityClass = entityType.getEntityClass();
        AbstractIdentifiableObject value = DefaultJacksonMapperProvider.getObjectMapper().convertValue(entity, entityClass);
        Accessor<AbstractIdentifiableObject> accessor = (Accessor<AbstractIdentifiableObject>) entityType.getAccessor();
        AbstractIdentifiableObject save = accessor.save(value);
        return Response.status(200).entity(save).build();
    }
}
