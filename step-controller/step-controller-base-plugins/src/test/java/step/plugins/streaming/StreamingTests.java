package step.plugins.streaming;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import step.streaming.client.download.WebsocketDownload;
import step.streaming.client.upload.StreamingUpload;
import step.streaming.common.StreamingResourceMetadata;
import step.streaming.common.StreamingResourceReference;
import step.streaming.common.StreamingResourceStatus;
import step.streaming.common.StreamingResourceTransferStatus;
import step.streaming.data.MD5CalculatingOutputStream;
import step.streaming.websocket.client.upload.WebsocketUploadProvider;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/* These tests must be manually performed with an already running server (assumed at localhost:8080)
... and they're probably broken by now because of the introduction of access management ;-)
 */
@SuppressWarnings("resource")
@Ignore
public class StreamingTests {

    @Test
    public void testWebsocketUpload() throws Exception {
        // NOTE: this does not have the possibility to create an upload context, so it will be null.
        URI uploadUri = URI.create("ws://localhost:8080" + StreamingResourcesControllerPlugin.UPLOAD_PATH);
        StreamingResourceMetadata metadata = new StreamingResourceMetadata("streaming-test.txt", "text/plain");
        File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("streaming-test.txt")).getFile());


        StreamingUpload upload = new WebsocketUploadProvider(uploadUri).startLiveTextFileUpload(file, metadata, StandardCharsets.UTF_8);
        StreamingResourceStatus status = upload.signalEndOfInput().get();
        Assert.assertEquals(StreamingResourceTransferStatus.COMPLETED, status.getTransferStatus());
        Assert.assertEquals(24, status.getCurrentSize().intValue());
        System.err.println("URI: " + upload.getReference());
    }

    @Test
    public void testDownload() throws Exception {
        boolean print = true;
        // Use URI as given by testUpload
        String url = "ws://localhost:8080/ws/streaming/download/688096be31d33d7851db515a";
        WebsocketDownload download = new WebsocketDownload(new StreamingResourceReference(URI.create(url)));
        OutputStream leafOut = print? System.err : OutputStream.nullOutputStream();
        MD5CalculatingOutputStream out = new MD5CalculatingOutputStream(leafOut);
        download.getInputStream().transferTo(out);
        out.close();
        download.close();
        Assert.assertEquals("47781eae31e76167871810077e05dbad", out.getChecksum());
    }
}
