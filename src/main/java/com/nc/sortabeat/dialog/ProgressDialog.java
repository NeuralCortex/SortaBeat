package com.nc.sortabeat.dialog;

import com.nc.sortabeat.Globals;
import java.util.ResourceBundle;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.*;

/**
 * 
 * @author Neural Cortex
 */
public class ProgressDialog extends Stage {

    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label label = new Label("Preparing...");
    private final Button cancelBtn = new Button("Cancel");

    public ProgressDialog(ResourceBundle bundle,Stage owner, int total) {
        setTitle(bundle.getString("dlg.progress.title"));
        cancelBtn.setText(bundle.getString("btn.cancel"));
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        initStyle(StageStyle.UTILITY);

        progressBar.setPrefWidth(380);

        VBox root = new VBox(18, label, progressBar, cancelBtn);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(Globals.CSS_PATH);
        setScene(scene);
        sizeToScene();

        setOnShown(e -> centerOnOwner(owner));

        cancelBtn.setOnAction(ev -> fireEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSE_REQUEST)));
    }

    private void centerOnOwner(Stage owner) {
        if (owner == null) {
            centerOnScreen();
            return;
        }

        double centerX = owner.getX() + owner.getWidth() / 2;
        double centerY = owner.getY() + owner.getHeight() / 2;

        setX(centerX - getWidth() / 2);
        setY(centerY - getHeight() / 2);
    }

    public StringProperty messageProperty() {
        return label.textProperty();
    }

    public DoubleProperty progressProperty() {
        return progressBar.progressProperty();
    }

    public void setOnCancel(Runnable action) {
        cancelBtn.setOnAction(e -> {
            action.run();
            close();
        });
    }
}