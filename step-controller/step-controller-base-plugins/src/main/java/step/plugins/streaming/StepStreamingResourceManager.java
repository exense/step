package step.plugins.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.access.User;
import step.core.deployment.AuthorizationException;
import step.core.objectenricher.ObjectHookRegistry;
import step.framework.server.Session;
import step.framework.server.access.AuthorizationManager;
import step.streaming.common.StreamingResourceReference;
import step.streaming.common.StreamingResourceUploadContexts;
import step.streaming.server.DefaultStreamingResourceManager;
import step.streaming.server.StreamingResourcesStorageBackend;

import java.util.function.Function;

public class StepStreamingResourceManager extends DefaultStreamingResourceManager {
    private static final Logger logger = LoggerFactory.getLogger(StepStreamingResourceManager.class);
    static final String ATTRIBUTE_STEP_SESSION = "stepSession";


    private final AuthorizationManager<User, Session<User>> authorizationManager;
    private final ObjectHookRegistry objectHookRegistry;

    @SuppressWarnings("unchecked")
    public StepStreamingResourceManager(GlobalContext globalContext, StreamingResourceCollectionCatalogBackend catalog, StreamingResourcesStorageBackend storage, Function<String, StreamingResourceReference> referenceProducerFunction, StreamingResourceUploadContexts uploadContexts) {
        super(catalog, storage, referenceProducerFunction, uploadContexts);

        authorizationManager = globalContext.get(AuthorizationManager.class);
        objectHookRegistry = globalContext.get(ObjectHookRegistry.class);
        if (authorizationManager == null || objectHookRegistry == null) {
            // this shouldn't happen in production, but may be the case in some unit tests where it's harmless and not required
            logger.warn("AuthorizationManager and/or ObjectHookRegistry missing from context, all permission checks will refuse access");
        }
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

    public void checkDownloadPermission(StreamingResource resource, Session<User> stepSession) throws AuthorizationException {
        if (authorizationManager == null || objectHookRegistry == null) {
            throw new AuthorizationException("Access denied - unable to check permissions");
        }
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
