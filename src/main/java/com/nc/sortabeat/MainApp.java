package com.nc.sortabeat;

import com.nc.sortabeat.controller.MainController;
import com.nc.sortabeat.tools.HelperFunctions;
import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javax.swing.UIManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

/**
 *
 * @author Neural Cortex
 */
public class MainApp extends Application {

    private static final Logger _log = LogManager.getLogger(MainApp.class);
    private final ResourceBundle bundle = ResourceBundle.getBundle(Globals.BUNDLE_PATH, Globals.DEFAULT_LOCALE);
    private MainController mainController;

    @Override
    public void start(Stage stage) throws Exception {
        initLogger(Globals.LOG4J2_CONFIG_PATH);

        Locale.setDefault(Globals.DEFAULT_LOCALE);
        saveDefaultConfig();

        FXMLLoader loader = new FXMLLoader(getClass().getResource(Globals.FXML_MAIN_PATH), bundle);
        mainController = new MainController(stage);
        loader.setController(mainController);
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(Globals.CSS_PATH);
        stage.getIcons().add(new Image(MainApp.class.getResource(Globals.PNG_LOGO).toString()));

        stage.setTitle(bundle.getString("app.name") + " " + MainApp.class.getPackage().getImplementationVersion());
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            saveState(stage);
        });

        if (!restoreState(stage)) {
            HelperFunctions.centerWindow(stage);
        }

        stage.show();
    }

    private void initLogger(String path) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.setConfigLocation(new File(path).toURI());
    }

    private void saveDefaultConfig() {
        Globals.propman.setProperty(Globals.PREFIX_LENGTH, Globals.propman.getProperty(Globals.PREFIX_LENGTH, "3"));
        Globals.propman.setProperty(Globals.SCROLL_SPEED, Globals.propman.getProperty(Globals.SCROLL_SPEED, "0.001"));
        Globals.propman.save();
    }

    private void saveState(Stage stage) {
        Globals.propman.setProperty(Globals.WIN_X, String.valueOf(stage.getX()));
        Globals.propman.setProperty(Globals.WIN_Y, String.valueOf(stage.getY()));
        Globals.propman.setProperty(Globals.WIN_WIDTH, String.valueOf(stage.getWidth()));
        Globals.propman.setProperty(Globals.WIN_HEIGHT, String.valueOf(stage.getHeight()));
        Globals.propman.setProperty(Globals.VOLUME, String.valueOf(mainController.getVolume()));
        Globals.propman.save();
    }

    private boolean restoreState(Stage stage) {
        if (Globals.propman.containsKey(Globals.WIN_X)) {
            stage.setX(Double.parseDouble(Globals.propman.getProperty(Globals.WIN_X)));
            stage.setY(Double.parseDouble(Globals.propman.getProperty(Globals.WIN_Y)));
            stage.setWidth(Double.parseDouble(Globals.propman.getProperty(Globals.WIN_WIDTH)));
            stage.setHeight(Double.parseDouble(Globals.propman.getProperty(Globals.WIN_HEIGHT)));
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        System.setProperty("prism.marlin", "false");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            _log.error(ex.getMessage());
        }
        launch(args);
    }
}
