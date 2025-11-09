package com.nc.sortabeat.tools;

import com.nc.sortabeat.pojo.AudioFile;
import java.io.IOException;
import java.util.Arrays;
import java.util.ResourceBundle;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HelperFunctions {

    private static final Logger _log = LogManager.getLogger(HelperFunctions.class);
    private static final double DEFAULT_TABLE_WIDTH = 500.0;

    public static void centerWindow(Window window) {
        window.addEventHandler(WindowEvent.WINDOW_SHOWN, (WindowEvent event) -> {
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            window.setX((screenBounds.getWidth() - window.getWidth()) / 2);
            window.setY((screenBounds.getHeight() - window.getHeight()) / 2);
        });
    }

    public static void centerStageOnOwner(Stage child, Stage owner) {
        child.setOnShown(e -> {
            double centerX = owner.getX() + owner.getWidth() / 2;
            double centerY = owner.getY() + owner.getHeight() / 2;
            child.setX(centerX - child.getWidth() / 2);
            child.setY(centerY - child.getHeight() / 2);
        });
    }

    public Node loadFxml(ResourceBundle bundle, String path, Object controller) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path), bundle);
            loader.setController(controller);
            Node node = loader.load();
            return node;
        } catch (IOException ex) {
            _log.error(ex.getMessage());
        }
        return null;
    }

    public static Tab addTab(ResourceBundle bundle, TabPane tabPane, String path, Object controller, String tabName) {
        long start = System.currentTimeMillis();
        Tab tab = new Tab(tabName);
        tabPane.getTabs().add(tab);
        HelperFunctions helperFunctions = new HelperFunctions();
        Node node = helperFunctions.loadFxml(bundle, path, controller);
        node.setUserData(controller);
        tab.setContent(node);
        long end = System.currentTimeMillis();
        System.out.println("Loadtime (" + controller.toString() + ") in ms: " + (end - start));
        return tab;
    }

    public static BorderPane createTab(ResourceBundle bundle, String path, Object controller) {
        long start = System.currentTimeMillis();
        BorderPane borderPane = new BorderPane();
        HelperFunctions helperFunctions = new HelperFunctions();
        Node node = helperFunctions.loadFxml(bundle, path, controller);
        node.setUserData(controller);
        borderPane.setCenter(node);
        long end = System.currentTimeMillis();
        System.out.println("Loadtime (" + controller.toString() + ") in ms: " + (end - start));
        return borderPane;
    }

    public static Node addPlugin(ResourceBundle bundle, String path, Object controller) {
        long start = System.currentTimeMillis();
        HelperFunctions helperFunctions = new HelperFunctions();
        Node node = helperFunctions.loadFxml(bundle, path, controller);
        node.setUserData(controller);
        long end = System.currentTimeMillis();
        System.out.println("Loadtime (" + controller.toString() + ") in ms: " + (end - start));
        return node;
    }

    public static void resizeColumns(BorderPane borderPane, TableView<AudioFile> table, double[] widths) {
        if (widths == null || widths.length == 0) {
            _log.warn("No widths provided for column resizing.");
            return;
        }

        ObservableList<TableColumn<AudioFile, ?>> columns = table.getColumns();
        int numColumns = columns.size();
        double tableWidth = table.getWidth();

        // Use fallback width if table width is 0 (e.g., during initialization)
        if (tableWidth <= 0) {
            tableWidth = borderPane.getWidth() > 0 ? borderPane.getWidth() : DEFAULT_TABLE_WIDTH;
            _log.warn("Table width is 0; using fallback width: " + tableWidth);
        }

        // Use provided percentage widths, or default to equal distribution if array is too short
        double[] effectivePercentages = new double[numColumns];
        double defaultPercentage = 100.0 / numColumns;
        for (int i = 0; i < numColumns; i++) {
            if (i < widths.length && widths[i] > 0) {
                effectivePercentages[i] = widths[i];
            } else {
                effectivePercentages[i] = defaultPercentage;
                _log.warn("Invalid or missing percentage for column " + i + "; using default percentage: " + defaultPercentage);
            }
        }

        // Normalize percentages to sum to 100%
        double totalPercentage = Arrays.stream(effectivePercentages).sum();
        if (Math.abs(totalPercentage - 100.0) > 0.01 && totalPercentage > 0) {
            double scaleFactor = 100.0 / totalPercentage;
            for (int i = 0; i < effectivePercentages.length; i++) {
                effectivePercentages[i] *= scaleFactor;
            }
            _log.info("Normalized percentages to sum to 100%; scale factor: " + scaleFactor);
        }

        // Convert percentages to pixel widths based on table width
        double[] pixelWidths = new double[numColumns];
        for (int i = 0; i < numColumns; i++) {
            pixelWidths[i] = (effectivePercentages[i] / 100.0) * tableWidth;
            // Ensure minimum width to prevent unreadable columns
            pixelWidths[i] = Math.max(pixelWidths[i], 30.0); // Minimum 30 pixels
        }

        // Apply pixel widths to columns
        for (int i = 0; i < numColumns; i++) {
            columns.get(i).setPrefWidth(pixelWidths[i]);
            _log.info("Set column " + columns.get(i).getText() + " width to: " + pixelWidths[i] + " pixels (" + effectivePercentages[i] + "%)");
        }
    }

    public static void setupImage(Button button, Image image, int size) {
        ImageView iv = new ImageView(image);
        iv.setFitWidth(size);
        iv.setFitHeight(size);

        button.setGraphic(iv);
        button.setText("");
    }

    public static <S, T> void centerColumn(TableColumn<S, T> column) {
        column.setCellFactory(tc -> new TableCell<S, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.toString());
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }
}
