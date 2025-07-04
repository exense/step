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
import step.streaming.websocket.protocol.download.DownloadProtocolMessage;
import step.streaming.websocket.protocol.upload.UploadProtocolMessage;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.Objects;

/* For now, these tests must be manually performed with an already running server (assumed at localhost:8080)
 */
@Ignore
public class StreamingTests {

    @Test
    public void testUpload() throws Exception {
        URI uploadUri = URI.create("ws://localhost:8080" + StreamingResourcesControllerPlugin.UPLOAD_PATH);
        StreamingResourceMetadata metadata = new StreamingResourceMetadata("test.txt", "text/plain");
        File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("test.txt")).getFile());


        StreamingUpload upload = new WebsocketUploadProvider(uploadUri).startLiveFileUpload(file, metadata);
        StreamingResourceStatus status = upload.signalEndOfInput().get();
        Assert.assertEquals(StreamingResourceTransferStatus.COMPLETED, status.getTransferStatus());
        Assert.assertEquals(24, status.getCurrentSize().intValue());
        System.err.println("URI: " + upload.getReference());
    }

    @Test
    public void testDownload() throws Exception {
        // Use URI as given by testUpload
        String url = "ws://localhost:8080/ws/streaming/download/68652f2bd9657b12cd4f9920";
        WebsocketDownload download = new WebsocketDownload(new StreamingResourceReference(URI.create(url)));
        MD5CalculatingOutputStream out = new MD5CalculatingOutputStream(OutputStream.nullOutputStream());
        download.getInputStream().transferTo(out);
        out.close();
        download.close();
        Assert.assertEquals("9095e9bdc5f12c1938da58cd068c037c", out.getChecksum());
    }
}
