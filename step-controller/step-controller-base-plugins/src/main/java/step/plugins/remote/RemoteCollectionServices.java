package step.plugins.remote;

import step.client.collections.remote.FindRequest;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.tables.TableRegistry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Stream;

@Singleton
@Path("remote")
public class RemoteCollectionServices<T> extends AbstractServices {

    protected TableRegistry tableRegistry;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        tableRegistry = getContext().get(TableRegistry.class);
    }

    @PreDestroy
    public void destroy() {
    }

    @POST
    @Path("/{id}/find")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right="plan-write")
    public  Stream<T> find(@PathParam("id") String collectionId, FindRequest findRequest) {
        Collection<T> collectionDriver = (Collection<T>) tableRegistry.get(collectionId).getCollectionDriver();
        return  collectionDriver.find(findRequest.getFilter(), findRequest.getOrder(), findRequest.getSkip(),
                findRequest.getLimit(), findRequest.getMaxTime());

    }

    @GET
    @Path("/{id}/distinct/{columnName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right="plan-write")
    public  List<String> distinct(@PathParam("id") String collectionId, @PathParam("id") String columnName) {
        Collection<T> collectionDriver = (Collection<T>) tableRegistry.get(collectionId).getCollectionDriver();
        return collectionDriver.distinct(columnName);
    }

    @POST
    @Path("/{id}/distinct/{columnName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right="plan-write")
    public  List<String> distinctPost(@PathParam("id") String collectionId, @PathParam("id") String columnName, Filter filter) {
        Collection<T> collectionDriver = (Collection<T>) tableRegistry.get(collectionId).getCollectionDriver();
        return collectionDriver.distinct(columnName,filter);
    }

    @POST
    @Path("/{id}/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right="plan-write")
    public void delete(@PathParam("id") String collectionId, Filter filter) {
        Collection<T> collectionDriver = (Collection<T>) tableRegistry.get(collectionId).getCollectionDriver();
        collectionDriver.remove(filter);
    }

    @POST
    @Path("/{id}/save")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right="plan-write")
    public  T save(@PathParam("id") String collectionId, T entity) {
        Collection<T> collectionDriver = (Collection<T>) tableRegistry.get(collectionId).getCollectionDriver();
        return collectionDriver.save(entity);
    }

    @POST
    @Path("/{id}/save")
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right="plan-write")
    public  void saveBulk(@PathParam("id") String collectionId, List<T> entities) {
        Collection<T> collectionDriver = (Collection<T>) tableRegistry.get(collectionId).getCollectionDriver();
        collectionDriver.save(entities);
    }





}
