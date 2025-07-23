package step.plugins.streaming;

import step.core.GlobalContext;
import step.core.access.User;
import step.core.deployment.AuthorizationException;
import step.core.objectenricher.ObjectHookRegistry;
import step.framework.server.Session;
import step.framework.server.access.AuthorizationManager;
import step.streaming.common.StreamingResourceReference;
import step.streaming.common.StreamingResourceUploadContexts;
import step.streaming.server.DefaultStreamingResourceManager;
import step.streaming.server.StreamingResourcesCatalogBackend;
import step.streaming.server.StreamingResourcesStorageBackend;

import java.util.function.Function;

public class StepStreamingResourceManager extends DefaultStreamingResourceManager {
    static final String ATTRIBUTE_STEP_SESSION = "stepSession";


    private final AuthorizationManager<User, Session<User>> authorizationManager;
    private final ObjectHookRegistry objectHookRegistry;

    @SuppressWarnings("unchecked")
    public StepStreamingResourceManager(GlobalContext globalContext, StreamingResourcesCatalogBackend catalog, StreamingResourcesStorageBackend storage, Function<String, StreamingResourceReference> referenceProducerFunction, StreamingResourceUploadContexts uploadContexts) {
        super(catalog, storage, referenceProducerFunction, uploadContexts);
        authorizationManager = globalContext.require(AuthorizationManager.class);
        objectHookRegistry = globalContext.require(ObjectHookRegistry.class);
    }

    public StreamingResourceCollectionCatalogBackend getCatalog() {
        return (StreamingResourceCollectionCatalogBackend) catalog;
    }

    public StreamingResourcesStorageBackend getStorage() {
        return storage;
    }

    public void checkDownloadPermission(String resourceId, Session<User> stepSession) {
        checkDownloadPermission(getCatalog().getEntity(resourceId), stepSession);
    }

    public void checkDownloadPermission(StreamingResource resource, Session<User> stepSession) {
        try {
            if (stepSession == null || !stepSession.isAuthenticated()) {
                throw new AuthorizationException("Access denied - not authenticated");
            }
            // This will check that the user has resource-read rights in whatever his current context is
            if (!authorizationManager.checkRightInContext(stepSession, "resource-read")) {
                throw new AuthorizationException("Access denied - insufficient rights");
            }
            // ... and this will check that the current context actually allows access to the object
            if (!objectHookRegistry.getObjectPredicate(stepSession).test(resource)) {
                throw new AuthorizationException("Access denied - wrong session context");
            }
        } catch (AuthorizationException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthorizationException(e.getMessage());
        }
    }
}
