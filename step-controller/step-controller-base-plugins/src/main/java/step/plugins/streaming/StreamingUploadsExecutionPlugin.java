package step.plugins.streaming;

import step.constants.LiveReportingConstants;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.streaming.common.StreamingResourceUploadContexts;

import java.util.Objects;

/**
 * Makes the streaming uploads available to the executions: the keywords they call get where to upload the files they
 * stream to, and the report nodes of the calls the files as attachments. Used by the controller and by the local
 * executions of the CLI.
 */
public class StreamingUploadsExecutionPlugin extends AbstractExecutionEnginePlugin {

    private final StepStreamingResourceManager manager;
    private final StreamingResourceUploadContexts uploadContexts;
    private final String controllerUrl;

    /**
     * @param controllerUrl the URL of the server receiving the uploads, and the live measures, of the keywords
     */
    public StreamingUploadsExecutionPlugin(StepStreamingResourceManager manager, StreamingResourceUploadContexts uploadContexts,
                                           String controllerUrl) {
        this.manager = Objects.requireNonNull(manager);
        this.uploadContexts = Objects.requireNonNull(uploadContexts);
        this.controllerUrl = Objects.requireNonNull(controllerUrl);
    }

    @Override
    public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
        // Makes streaming available to the execution
        executionContext.put(StreamingResourceUploadContexts.class, uploadContexts);
        executionContext.put(LiveReportingConstants.STREAMING_WEBSOCKET_UPLOAD_PATH, StreamingResourcesControllerPlugin.UPLOAD_PATH);
        // Note: this URL is also used by the measures streaming to determine the hostname.
        executionContext.put(LiveReportingConstants.LIVEREPORTING_CONTROLLER_URL, controllerUrl);
    }

    @Override
    public void afterExecutionEnd(ExecutionContext context) {
        // unregisters the execution with manager (registration is on-demand,
        // but execution end needs to be signaled explicitly)
        manager.unregisterExecution(context.getExecutionId());
    }
}
