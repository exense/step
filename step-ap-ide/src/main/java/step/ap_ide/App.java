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

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {

        WebView webView = new WebView();

        // 1. Route JS alerts to Java standard error
        webView.getEngine().setOnAlert(event -> System.err.println("JS ALERT/ERR: " + event.getData()));

        // Catch standard JavaFX JS errors
        webView.getEngine().setOnError(event -> System.err.println("JS Error: " + event.getMessage()));

        // 2. Track load states safely
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            System.out.println("Load state: " + newState);
            if (newState == Worker.State.FAILED) {
                System.err.println("Network Error: " + webView.getEngine().getLoadWorker().getException());
            }
        });

        // 3. Inject JS error handler exactly when the Document object is created
        webView.getEngine().documentProperty().addListener((obs, oldDoc, newDoc) -> {
            if (newDoc != null) {
                try {
                    webView.getEngine().executeScript("window.onerror = function(msg, url, line) { alert(msg + ' at line ' + line); };");
                } catch (Exception e) {
                    System.err.println("Could not inject error handler: " + e.getMessage());
                }
            }
        });

        // 4. Setup trivial MenuBar & ToolBar
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> System.exit(0));
        fileMenu.getItems().add(exitItem);
        menuBar.getMenus().add(fileMenu);

        ToolBar toolBar = new ToolBar();
        Button reloadButton = new Button("Reload");
        reloadButton.setOnAction(e -> webView.getEngine().reload());

        // NEW: Button to dump the HTML so we can see what's actually there
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

        toolBar.getItems().addAll(reloadButton, dumpHtmlButton);

        VBox topContainer = new VBox(menuBar, toolBar);
        BorderPane root = new BorderPane();
        root.setTop(topContainer);
        root.setCenter(webView);

        Scene scene = new Scene(root, 1024, 768);
        primaryStage.setTitle("Native Web App Wrapper");
        primaryStage.setScene(scene);
        primaryStage.show();
        webView.getEngine().load("http://127.0.0.1:4201");
    }

    public static void main(String[] args) {
        System.setProperty("com.sun.webkit.useHTTP2Loader", "false");
        launch(args);
    }
}
