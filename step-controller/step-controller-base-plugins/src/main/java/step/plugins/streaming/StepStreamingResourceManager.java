package step.plugins.streaming;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.constants.StreamingConstants;
import step.core.GlobalContext;
import step.core.access.User;
import step.core.deployment.AuthorizationException;
import step.core.objectenricher.ObjectHookRegistry;
import step.framework.server.Session;
import step.framework.server.access.AuthorizationManager;
import step.streaming.common.*;
import step.streaming.server.DefaultStreamingResourceManager;
import step.streaming.server.StreamingResourcesStorageBackend;

import java.io.IOException;
import java.util.function.Function;

public class StepStreamingResourceManager extends DefaultStreamingResourceManager {
    private static final Logger logger = LoggerFactory.getLogger(StepStreamingResourceManager.class);
    static final String ATTRIBUTE_STEP_SESSION = "stepSession";


    private final AuthorizationManager<User, Session<User>> authorizationManager;
    private final ObjectHookRegistry objectHookRegistry;
    private final QuotaCheckers quotaCheckers;

    @SuppressWarnings("unchecked")
    public StepStreamingResourceManager(GlobalContext globalContext, StreamingResourceCollectionCatalogBackend catalog, StreamingResourcesStorageBackend storage, Function<String, StreamingResourceReference> referenceProducerFunction, StreamingResourceUploadContexts uploadContexts) {
        super(catalog, storage, referenceProducerFunction, uploadContexts);

        authorizationManager = globalContext.get(AuthorizationManager.class);
        objectHookRegistry = globalContext.get(ObjectHookRegistry.class);
        if (authorizationManager == null || objectHookRegistry == null) {
            // this shouldn't happen in production, but may be the case in some unit tests where it's harmless and not required
            logger.warn("AuthorizationManager and/or ObjectHookRegistry missing from context, all permission checks will refuse access");
        }

        QuotaLimits globalLimits = QuotaLimits.fromStepProperties(globalContext.getConfiguration());
        logger.info("Streaming attachment global quotas (null==unlimited): {}", globalLimits);
        quotaCheckers = new QuotaCheckers(globalLimits);
    }

    @Override
    public boolean isUploadContextRequired() {
        // require upload context information on upload. This will reject uploads without a valid context id.
        return true;
    }

    @Override
    public String registerNewResource(StreamingResourceMetadata metadata, String uploadContextId) throws QuotaExceededException, IOException {
        // guaranteed to exist because we require upload context
        StreamingResourceUploadContext uploadContext = uploadContexts.getContext(uploadContextId);
        String executionId = (String) uploadContext.getAttributes().get(StreamingConstants.AttributeNames.RESOURCE_EXECUTION_ID);
        QuotaChecker quotaChecker = quotaCheckers.getForExecution(executionId, uploadContext);
        if (quotaChecker != null) {
            // This will throw a QuotaExceededException if quota would be exceeded. We want to avoid even creating an actual resource in this case.
            String reservation;
            try {
                reservation = quotaChecker.reserveNewResource();
            } catch (QuotaExceededException e) {
                uploadContexts.onResourceCreationRefused(uploadContextId, metadata, e.getMessage());
                throw e;
            }
            try {
                String resourceId = super.registerNewResource(metadata, uploadContextId);
                // bind resource in checker for tracking
                quotaChecker.bindResourceId(reservation, resourceId);
                // register this tracker for quick lookup for per-resource updates
                quotaCheckers.setForResource(resourceId, quotaChecker);
                return resourceId;
            } catch (IOException e) {
                quotaChecker.cancelReservation(reservation);
                throw e;
            }
        } else {
            return super.registerNewResource(metadata, uploadContextId);
        }
    }

    @Override
    protected void onSizeChanged(String resourceId, long currentSize) throws QuotaExceededException {
        QuotaChecker quotaChecker = quotaCheckers.getForResource(resourceId);
        if (quotaChecker != null) {
            // throws QuotaExceededException when quotas are exceeded.
            // Any other kind of exception would probably be caused by a bug somewhere...
            quotaChecker.onSizeChanged(resourceId, currentSize);
        }
    }

    // Required to know when executions are finished, so quota checkers can be cleaned up.
    // "registration" is done automatically and on the fly.
    void unregisterExecution(String executionId) {
        quotaCheckers.removeExecution(executionId);
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
