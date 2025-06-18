package step.streaming.websocket.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import jakarta.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.agent.JsonMessageCodec;
import step.grid.io.stream.JsonMessage;
import step.grid.io.stream.StreamableResourceStatus;
import step.grid.io.stream.download.RequestChunkMessage;
import step.grid.io.stream.download.ResourceStatusChangedMessage;
import step.streaming.websocket.StreamableResourcesDownloadEndpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

// This class is UGLY, I know. It's just a PoC.
public class StreamableResourcesDownloadClient implements Runnable {
    public static void main(String[] args) {
        // disable logging here, otherwise Websocket implementation spams debug logs.
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ((ch.qos.logback.classic.Logger) rootLogger).setLevel(Level.INFO);

        JsonMessage.setCodecIfRequired(JsonMessageCodec::new);

        String endpointUrl = "ws://localhost:8080" + StreamableResourcesDownloadEndpoint.ENDPOINT_URL_WITHOUT_ID;
        String input = null;
        if (args.length > 0) {
            input = args[0];
            // a comment in our arguments :-)
            if (input.startsWith("#")) {
                input = null;
            }
        }
        if (input == null) {
            System.out.println("Enter ID to append to " + endpointUrl + ", or complete download endpoint URL");
            Scanner scanner = new Scanner(System.in);
            input = scanner.nextLine();
        }
        if (input.startsWith("ws")) {
            endpointUrl = input;
        } else {
            endpointUrl += input;
        }
        System.out.println("Connecting to" + endpointUrl);
        new StreamableResourcesDownloadClient(endpointUrl).run();
    }

    private final String endpointUrl;
    private final StreamableResourcesDownloadClient client = this;
    private Session session;
    private final CompletableFuture<CloseReason> closeReason = new CompletableFuture<>();

    private long totalBytesReceived = 0;
    //private ResourceStatusChangedMessage lastMessage = null;

    public StreamableResourcesDownloadClient(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    @Override
    public void run() {
        try {
            connect();
            CloseReason reason = closeReason.get();
            System.out.println("Session closed: " + reason);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeIfFinished(ResourceStatusChangedMessage message) {
        boolean close = false;
        if (message.status == StreamableResourceStatus.FINISHED) {
            close = totalBytesReceived == message.currentSize;
        } else if (message.status == StreamableResourceStatus.FAILED) {
            close = true;
        }
        if (close) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "client closing: " + message.status + ", bytes received: " + totalBytesReceived));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class State {}
    private static final State ExpectingMessage = new State();
    private static class ExpectingData extends State {
        final ResourceStatusChangedMessage message;
        ExpectingData(ResourceStatusChangedMessage message) {
            this.message = message;
        }
    }

    private State state = ExpectingMessage;
    private ResourceStatusChangedMessage queuedMessage = null;

    // NOTE: This is effectively a single threaded implementation (guaranteed by the framework), so it's safe to
    // use a shared state between onMessage and onData.
    private void onMessage(ResourceStatusChangedMessage message, boolean reinjected) {
        System.out.println("Working on " + message +", reinjected=" + reinjected);
        // we might receive multiple messages in quick succession, so ensure that we only work on the latest one
        if (state == ExpectingMessage) {
            // we may already have received all data, so close if only the status (but not the size) changed.
            closeIfFinished(message);
            state = new ExpectingData(message);
            session.getAsyncRemote().sendText(new RequestChunkMessage(totalBytesReceived, message.currentSize).toString());
        } else {
            // we're already handling data, queue the message. In case another message is already queued, just replace it with the newest status
            System.out.println("enqueuing message: " + message);
            queuedMessage = message;
        }
    }

    private void onData(InputStream inputStream) {
        ExpectingData data = (ExpectingData) state; // this would throw an exception if we're in the wrong state, but that shouldn't happen.
        System.out.println("Receiving data...");
        try {
            // we're not writing to a file, just a dummy stream.
            long read = inputStream.transferTo(OutputStream.nullOutputStream());
            totalBytesReceived += read;
            System.out.println("Transferred: " + read +", total bytes received: " + totalBytesReceived);
            inputStream.close();
        } catch (IOException e) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, "Client error"));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        closeIfFinished(data.message);
        state = ExpectingMessage;
        if (queuedMessage != null) {
            ResourceStatusChangedMessage reinject = queuedMessage;
            queuedMessage = null;
            onMessage(reinject, true);
        }
    }


    public Session connect() throws Exception {
        var container = ContainerProvider.getWebSocketContainer();

        Endpoint endpoint = new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig config) {
                client.session = session;

                session.addMessageHandler(String.class, msg -> client.onMessage(JsonMessage.fromString(msg), false));
                session.addMessageHandler(InputStream.class, client::onData);
            }

            @Override
            public void onClose(Session session, CloseReason closeReason) {
                client.closeReason.complete(closeReason);
            }

            @Override
            public void onError(Session session, Throwable thr) {
                client.closeReason.completeExceptionally(thr);
            }
        };

        return container.connectToServer(endpoint, ClientEndpointConfig.Builder.create().build(), URI.create(client.endpointUrl));
    }
}
