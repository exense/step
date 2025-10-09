package step.plugins.streaming;

import ch.exense.commons.io.FileHelper;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import step.constants.LiveReportingConstants;
import step.core.GlobalContext;
import step.core.collections.CollectionFactory;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.streaming.common.*;
import step.streaming.server.FilesystemStreamingResourcesStorageBackend;
import step.streaming.server.StreamingResourcesStorageBackend;
import step.streaming.util.ThreadPools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@Ignore
public class StepStreamingResourceManagerTests {

    private File storageDirectory;
    private GlobalContext globalContext;

    private static List<Path> findMatchingFiles(File rootDir, String regex) throws IOException {
        Pattern pattern = Pattern.compile(regex);
        List<Path> matchingFiles = new ArrayList<>();

        try (var stream = Files.walk(rootDir.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(path -> pattern.matcher(path.getFileName().toString()).matches())
                    .forEach(matchingFiles::add);
        }

        return matchingFiles;
    }

    @Before
    public void before() throws Exception {
        storageDirectory = Files.createTempDirectory("step-streaming-storage-").toFile();
        globalContext = new GlobalContext();
        globalContext.put(CollectionFactory.class, new InMemoryCollectionFactory(new Properties()));
    }

    @After
    public void after() throws Exception {
        FileHelper.deleteFolder(storageDirectory);
    }

    @Test
    public void testCreationAndFindAndDeletion() throws Exception {
        StreamingResourceUploadContexts uploadContexts = new StreamingResourceUploadContexts();
        StreamingResourceCollectionCatalogBackend catalog = new StreamingResourceCollectionCatalogBackend(globalContext);
        StreamingResourcesStorageBackend storage = new FilesystemStreamingResourcesStorageBackend(storageDirectory);
        Function<String, StreamingResourceReference> refProducer = resourceId -> new StreamingResourceReference(URI.create("http://dummy/" + resourceId));
        StepStreamingResourceManager manager = new StepStreamingResourceManager(globalContext, catalog, storage, refProducer, uploadContexts, ThreadPools.createPoolExecutor("ws-upload-processor"));

        // Simulate an upload; we basically manually do the steps that the framework normally does
        StreamingResourceUploadContext uploadContext = new StreamingResourceUploadContext();
        uploadContexts.registerContext(uploadContext);
        String executionId = new ObjectId().toHexString();
        uploadContext.getAttributes().put(LiveReportingConstants.CONTEXT_EXECUTION_ID, executionId);
        String resourceId = manager.registerNewResource(new StreamingResourceMetadata("dummy.txt", StreamingResourceMetadata.CommonMimeTypes.TEXT_PLAIN, true), uploadContext.contextId);

        // Write data
        manager.writeChunk(resourceId, new ByteArrayInputStream("line1\nline2".getBytes()), true);
        manager.markCompleted(resourceId);

        assertEquals(new StreamingResourceStatus(StreamingResourceTransferStatus.COMPLETED, 11, 2L), manager.getStatus(resourceId));
        // Retrieve data; this will work because we're directly using the respective manager method.
        // For websocket/REST access, checkDownloadPermission is invoked by the implementations before actually retrieving the data.
        assertEquals(List.of("line1\n", "line2"), manager.getLines(resourceId, 0, 2).collect(Collectors.toList()));

        // Expecting: data file + index file
        assertEquals(2, findMatchingFiles(storageDirectory, resourceId + "\\..*").size());

        // Just for completeness, register another resource (contextId ist currently optional)
        String otherResourceId = manager.registerNewResource(new StreamingResourceMetadata("dummy.txt", StreamingResourceMetadata.CommonMimeTypes.TEXT_PLAIN, false), null);
        assertEquals(2, manager.getCatalog().accessor.stream().count());

        // This is what housekeeping does
        assertEquals(List.of(resourceId), manager.getCatalog().findResourceIdsForExecution(executionId).collect(Collectors.toList()));
        manager.deleteResource(resourceId);

        // Expecting only other resource left...
        assertEquals(List.of(otherResourceId), manager.getCatalog().accessor.stream().map(r -> r.getId().toHexString()).collect(Collectors.toList()));
        // ... and files deleted
        assertEquals(0, findMatchingFiles(storageDirectory, resourceId + "\\..*").size());
    }
}
