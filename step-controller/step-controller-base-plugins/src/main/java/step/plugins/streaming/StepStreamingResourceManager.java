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
    private final StreamingQuotaChecker quotaChecker;

    @SuppressWarnings("unchecked")
    public StepStreamingResourceManager(GlobalContext globalContext, StreamingResourceCollectionCatalogBackend catalog, StreamingResourcesStorageBackend storage, Function<String, StreamingResourceReference> referenceProducerFunction, StreamingResourceUploadContexts uploadContexts) {
        super(catalog, storage, referenceProducerFunction, uploadContexts);

        authorizationManager = globalContext.get(AuthorizationManager.class);
        objectHookRegistry = globalContext.get(ObjectHookRegistry.class);
        if (authorizationManager == null || objectHookRegistry == null) {
            // this shouldn't happen in production, but may be the case in some unit tests where it's harmless and not required
            logger.warn("AuthorizationManager and/or ObjectHookRegistry missing from context, all permission checks will refuse access");
        }

        // Quota enforcement (note we're internally talking about resources, but for users the term attachment is more meaningful)
        Configuration conf = globalContext.getConfiguration();
        Integer maxAttachmentsPerExecution = conf.getPropertyAsInteger("streaming.attachments.quota.maxAttachmentsPerExecution", 100);
        Long maxBytesPerAttachment = conf.getPropertyAsLong("streaming.attachments.quota.maxBytesPerAttachment", 100_000_000L);
        Long maxBytesPerExecution = conf.getPropertyAsLong("streaming.attachments.quota.maxBytesPerExecution", -1L);

        // handle "unlimited" case
        if (maxBytesPerAttachment != null && maxBytesPerAttachment < 0) maxBytesPerAttachment = null;
        if (maxBytesPerExecution != null && maxBytesPerExecution < 0) maxBytesPerExecution = null;
        if (maxAttachmentsPerExecution != null && maxAttachmentsPerExecution < 0) maxAttachmentsPerExecution = null;

        // Micro-optimization: only enable quota checker if at least one limit is to be enforced
        if (maxAttachmentsPerExecution != null || maxBytesPerAttachment != null || maxBytesPerExecution != null) {
            logger.info("Streaming attachment quotas (null==unlimited): maxBytesPerAttachment={} maxBytesPerExecution={} maxAttachmentsPerExecution={}",
                    maxBytesPerAttachment, maxBytesPerExecution, maxAttachmentsPerExecution);
            quotaChecker = new StreamingQuotaChecker(maxAttachmentsPerExecution, maxBytesPerAttachment, maxBytesPerExecution);
        } else {
            logger.info("Streaming attachment quota management disabled, all quotas are unlimited");
            quotaChecker = null;
        }
    }

    @Override
    public boolean isUploadContextRequired() {
        // require upload context information on upload. This will reject uploads without a valid context id.
        return true;
    }

    @Override
    public String registerNewResource(StreamingResourceMetadata metadata, String uploadContextId) throws QuotaExceededException, IOException {
        String executionId = (String) uploadContexts.getContext(uploadContextId).getAttributes().get(StreamingConstants.AttributeNames.RESOURCE_EXECUTION_ID);
        if (quotaChecker != null) {
            // This will throw a QuotaExceededException if quota would be exceeded. We want to avoid even creating an actual resource in this case.
            String reservation;
            try {
                reservation = quotaChecker.reserveNewResource(executionId);
            } catch (QuotaExceededException e) {
                uploadContexts.onResourceCreationRefused(uploadContextId, metadata, e.getMessage());
                throw e;
            }
            try {
                String resourceId = super.registerNewResource(metadata, uploadContextId);
                quotaChecker.bindResourceId(reservation, executionId, resourceId);
                return resourceId;
            } catch (IOException e) {
                quotaChecker.cancelReservation(executionId, reservation);
                throw e;
            }
        } else {
            return super.registerNewResource(metadata, uploadContextId);
        }
    }

    @Override
    protected void onSizeChanged(String resourceId, long currentSize) throws QuotaExceededException {
        if (quotaChecker != null) {
            // throws QuotaExceededException when quotas are exceeded.
            // Any other kind of exception would probably be caused by a bug somewhere...
            quotaChecker.onSizeChanged(resourceId, currentSize);
        }
    }

    // Required to know when executions are finished, so quota checker can clean up.
    // "registration" is done automatically and on the fly.
    void unregisterExecution(String executionId) {
        if (quotaChecker != null) {
            quotaChecker.unregisterExecution(executionId);
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
