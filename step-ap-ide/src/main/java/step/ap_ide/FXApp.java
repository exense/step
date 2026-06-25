package step.ap_ide;

import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;

public class FXApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(FXApp.class);

    private final WebView webView = new WebView();
    private final MenuBar menuBar = new MenuBar();
    private final ToolBar toolBar = new ToolBar();

    private void initWebView() {
        // Route JS alerts and errors to log
        webView.getEngine().setOnAlert(event -> logger.warn("JS Alert: {}", event.getData()));
        webView.getEngine().setOnError(event -> logger.error("JS Error: {}", event.getMessage()));

        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            logger.debug("Webview load state: " + newState);
            if (newState == Worker.State.FAILED) {
                logger.error("Webview Network Error: " + webView.getEngine().getLoadWorker().getException());
            }
        });

        if (false) {
            // Not sure what this was good for, but keeping it for now.
            // 3. Inject JS error handler exactly when the Document object is created
            webView.getEngine().documentProperty().addListener((obs, oldDoc, newDoc) -> {
                if (newDoc != null) {
                    try {
                        webView.getEngine().executeScript("window.onerror = function(msg, url, line) { alert(msg + ' at line ' + line); };");
                    } catch (Exception e) {
                        logger.error("Could not inject error handler: " + e.getMessage());
                    }
                }
            });
        }
    }

    private void initMenuBar() {
        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> System.exit(0));
        fileMenu.getItems().add(exitItem);
        menuBar.getMenus().add(fileMenu);
    }

    private void initToolBar() {
        Button reloadButton = new Button("Reload");
        reloadButton.setOnAction(e -> webView.getEngine().reload());

        Button dumpHtmlButton = new Button("Dump HTML");
        dumpHtmlButton.setOnAction(e -> {
            try {
                String html = (String) webView.getEngine().executeScript("document.documentElement.outerHTML");
                System.out.println("--- CURRENT HTML ---");
                System.out.println(html);
                System.out.println("--------------------");
            } catch (Exception ex) {
                System.err.println("Cannot read HTML yet: " + ex.getMessage());
            }
        });

        Button newEmptyApButton = new Button("New Empty AP");
        newEmptyApButton.setOnAction(e -> {
            try {
                File workDir = Files.createTempDirectory("automationPackageCollectionTest").toFile();
                System.err.println("CREATED AND NOW USING NEW WORKDIR: " + workDir.getAbsolutePath());
                StepUp.useAutomationPackageDirectory(workDir);
                webView.getEngine().reload();
            } catch (Exception ex) {
                logger.error("error", ex);
            }
        });

        toolBar.getItems().addAll(reloadButton, dumpHtmlButton, newEmptyApButton);
    }

    private Scene initScene() {
        VBox topContainer = new VBox(menuBar, toolBar);
        BorderPane root = new BorderPane();
        root.setTop(topContainer);
        root.setCenter(webView);

        return new Scene(root, 1600, 1000);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("StepUp");
        stage.setOnCloseRequest(event -> {
            logger.info("Window closed, terminating JVM...");
            System.exit(0);
        });

        initWebView();
        initMenuBar();
        initToolBar();

        stage.setScene(initScene());
        stage.show();

        webView.getEngine().load("http://127.0.0.1:4201");
    }

    public static void main(String[] args) {
        // absolutely required when using dev frontend, otherwise
        // FX webview will (wrongly) try to upgrade non-SSL HTTP requests to HTTP2,
        // and Frontend will (wrongly) never answer these upgrade requests. State of IT in 2026.
        System.setProperty("com.sun.webkit.useHTTP2Loader", "false");
        launch(args);
    }
}
