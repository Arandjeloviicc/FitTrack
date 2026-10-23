package com.fittrack.app;

import com.fittrack.config.AppConstants;
import com.fittrack.config.AppFonts;
import com.fittrack.ui.scene.SceneManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage stage) throws IOException {

        AppFonts.load();

        Platform.setImplicitExit(true);

        SceneManager.setStage(stage);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/fittrack/view/" + AppConstants.Views.LOGIN
                )
        );
        Scene scene = new Scene(loader.load(), 1280, 800);

        try {
            String iconPath =
                    "/com/fittrack/images/app/stage-icon.png";

            Image icon = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream(iconPath)
            ));

            if (icon.isError()) {
                throw new IllegalStateException("Failed to load Stage icon");
            }

            stage.getIcons().setAll(icon);

        } catch (RuntimeException exception) {
            log.error("Failed to load FitTrack Stage icon", exception);
        }

        stage.setTitle("FitTrack");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> Platform.exit());
        stage.show();
    }

    @Override
    public void stop() {
        System.out.println("FitTrack application stopped.");
    }

    public static void main(String[] args) {
        System.out.println("FitTrack application started.");
        launch(args);
    }
}