package step.plugins.streaming;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.AttachmentMeta;
import step.attachments.SkippedAttachmentMeta;
import step.attachments.StreamingAttachmentMeta;
import step.constants.LiveReportingConstants;
import step.core.GlobalContext;
import step.core.access.User;
import step.core.deployment.AuthorizationException;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectHookRegistry;
import step.framework.server.Session;
import step.framework.server.access.AuthorizationManager;
import step.resources.AttachmentStorage;
import step.streaming.common.QuotaExceededException;
import step.streaming.common.StreamingResourceMetadata;
import step.streaming.common.StreamingResourceReference;
import step.streaming.common.StreamingResourceUploadContext;
import step.streaming.common.StreamingResourceUploadContexts;
import step.streaming.server.DefaultStreamingResourceManager;
import step.streaming.server.StreamingResourcesStorageBackend;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public class StepStreamingResourceManager extends DefaultStreamingResourceManager implements AttachmentStorage {
    private static final Logger logger = LoggerFactory.getLogger(StepStreamingResourceManager.class);
    static final String ATTRIBUTE_STEP_SESSION = "stepSession";
    private static final Pattern MIME_TYPE_PATTERN = Pattern.compile("^[a-zA-Z0-9!#$&^_.+-]+/[a-zA-Z0-9!#$&^_.+-]+$");


    private final AuthorizationManager<User, Session<User>> authorizationManager;
    private final ObjectHookRegistry objectHookRegistry;
    private final QuotaCheckers quotaCheckers;

    @SuppressWarnings("unchecked")
    public StepStreamingResourceManager(GlobalContext globalContext,
                                        StreamingResourceCollectionCatalogBackend catalog,
                                        StreamingResourcesStorageBackend storage,
                                        Function<String, StreamingResourceReference> referenceProducerFunction,
                                        StreamingResourceUploadContexts uploadContexts) {
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
        String executionId = (String) uploadContext.getAttributes().get(LiveReportingConstants.CONTEXT_EXECUTION_ID);
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
            if (!authorizationManager.checkRightInContextIfDefined(stepSession, "resource-attachment-read")) {
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

    /* The following methods implement the AttachmentStorage interface */

    @Override
    public InputStream getAttachmentStream(String resourceId) throws Exception {
        return openStream(resourceId, 0, getStatus(resourceId).getCurrentSize());
    }

    @Override
    public AttachmentMeta saveAttachment(Object executionContext, byte[] content, String filename, String mimeType) {
        if (!(executionContext instanceof ExecutionContext)) {
            String className = executionContext == null ? "null" : executionContext.getClass().getName();
            return new SkippedAttachmentMeta(filename, mimeType, "UNEXPECTED: Invalid execution context type of class " + className);
        }
        ExecutionContext context = (ExecutionContext) Objects.requireNonNull(executionContext);
        StreamingResourceUploadContexts uploadContexts = context.get(StreamingResourceUploadContexts.class);

        if (uploadContexts == null) {
            return new SkippedAttachmentMeta(filename, mimeType, "UNEXPECTED: no StreamingResourceUploadContexts found in execution context");
        }

        // We don't need to react to any events, but we need an UploadContext to convey relevant contextual information about the attachment.
        StreamingResourceUploadContext uploadContext = new StreamingResourceUploadContext();
        ObjectEnricher enricher = context.getObjectEnricher();
        if (enricher != null) {
            uploadContext.getAttributes().put(LiveReportingConstants.ACCESSCONTROL_ENRICHER, enricher);
        }
        uploadContext.getAttributes().put(LiveReportingConstants.CONTEXT_EXECUTION_ID, context.getExecutionId());
        uploadContext.getAttributes().put(LiveReportingConstants.CONTEXT_VARIABLES_MANAGER, context.getVariablesManager());
        uploadContext.getAttributes().put(LiveReportingConstants.CONTEXT_REPORT_NODE, context.getCurrentReportNode());
        uploadContexts.registerContext(uploadContext);

        try {
            return createAttachmentFromContent(content, filename, mimeType, uploadContext.contextId);
        } catch (Exception e) {
            return new SkippedAttachmentMeta(filename, mimeType, e.getMessage());
        } finally {
            uploadContexts.unregisterContext(uploadContext);
        }
    }

    public AttachmentMeta createAttachmentFromContent(byte[] content, String filename, String mimeType, String uploadContextId) throws QuotaExceededException, IOException {
        mimeType = sanitizeMimeType(mimeType);
        boolean supportsLineAccess = isLineAccessSupported(mimeType);
        StreamingResourceMetadata metadata = new StreamingResourceMetadata(filename, mimeType, supportsLineAccess);
        String resourceId = registerNewResource(metadata, uploadContextId);
        StreamingAttachmentMeta meta = new StreamingAttachmentMeta(new ObjectId(resourceId), filename, mimeType);
        // Saving will be completely finished, in a single call, because all data is already present from the beginning.
        writeChunk(resourceId, new ByteArrayInputStream(content), true);
        var status = markCompleted(resourceId);
        meta.setCurrentNumberOfLines(status.getNumberOfLines());
        meta.setCurrentSize(status.getCurrentSize());
        meta.setStatus(StreamingAttachmentMeta.Status.COMPLETED);
        return meta;
    }

    private static String sanitizeMimeType(String mimeType) {
        if (mimeType == null) {
            // fallback if no mimeType was given
            return "application/octet-stream";
        }
        mimeType = mimeType.trim(); // just in case
        if (!MIME_TYPE_PATTERN.matcher(mimeType).matches()) {
            logger.warn("Invalid mime type \"{}\", replacing with generic application/octet-stream", mimeType);
            return "application/octet-stream";
        }
        return mimeType;
    }

    private static boolean isLineAccessSupported(String mimeType) {
        // Very simple heuristic for now, should catch > 90% of the useful cases
        // TODO: we can add a few other well-known textual formats if needed
        return mimeType.startsWith("text/");
    }
}
