package com.nc.sortabeat.controller;

import com.nc.sortabeat.Globals;
import com.nc.sortabeat.dialog.ProgressDialog;
import com.nc.sortabeat.pojo.AudioFile;
import com.nc.sortabeat.tools.HelperFunctions;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MainController class with optimized directory loading, lazy duration
 * calculation, drag-and-drop, MP3 playback, seek (drag, click, mouse wheel),
 * double-click, prev/next navigation, and table scrolling.
 */
public class MainController implements Initializable {
    
    @FXML
    private BorderPane borderPane;
    @FXML
    private TableView<AudioFile> table;
    @FXML
    private Button btnOpenDir;
    @FXML
    private Button btnPlay;
    @FXML
    private Button btnStop;
    @FXML
    private Button btnPause;
    @FXML
    private Button btnPrev;
    @FXML
    private Button btnNext;
    @FXML
    private Button btnRename;
    @FXML
    private Button btnRevert;
    @FXML
    private Button btnSaveAs;
    @FXML
    private Slider slVolume;
    @FXML
    private Slider slSeek;
    @FXML
    private Label lbNow;
    @FXML
    private Label lbDur;
    
    private static final Logger _log = LogManager.getLogger(MainController.class);
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
    
    private final Map<AudioFile, Thread> durationThreads = new ConcurrentHashMap<>();
    private final Map<AudioFile, CountDownLatch> durationLatches = new ConcurrentHashMap<>();
    private ObservableList<AudioFile> tableData;
    
    private final Stage stage;
    private final int size = 20;
    private final int prefixLength;
    
    private static final double SCROLL_THRESHOLD = 40.0;  // Pixels from edge to trigger scroll
    private static double SCROLL_SPEED = 0.001;      // Pixels per frame
    private static final Duration SCROLL_INTERVAL = Duration.millis(16); // ~60 FPS

    private Timeline autoScrollTimeline;
    private double dragY = 0;
    
    public MainController(Stage stage) {
        this.stage = stage;
        this.prefixLength = Integer.parseInt(Globals.propman.getProperty(Globals.PREFIX_LENGTH, "3"));
    }
    
    @Override
    public void initialize(URL location, ResourceBundle bundle) {
        borderPane.setPrefSize(Globals.WIDTH, Globals.HEIGHT);
        
        lbNow.setText("00:00");
        lbDur.setText("00:00");
        
        SCROLL_SPEED = Double.parseDouble(Globals.propman.getProperty(Globals.SCROLL_SPEED, "0.001"));
        
        MediaPlayerControl.setLbNow(lbNow);
        
        initTable(bundle);
        setupDragAndDrop();
        setupControls(bundle);
        
        Image imgOpen = new Image(MainController.class.getResource(Globals.PNG_OPEN).toString());
        HelperFunctions.setupImage(btnOpenDir, imgOpen, size);
        btnOpenDir.setOnAction(e -> openDir(bundle));
        
    }
    
    private void setupControls(ResourceBundle bundle) {
        Image imgPlay = new Image(MainController.class.getResource(Globals.PNG_PLAY).toString());
        Image imgStop = new Image(MainController.class.getResource(Globals.PNG_STOP).toString());
        Image imgPause = new Image(MainController.class.getResource(Globals.PNG_PAUSE).toString());
        Image imgPrev = new Image(MainController.class.getResource(Globals.PNG_PREVIOUS).toString());
        Image imgNext = new Image(MainController.class.getResource(Globals.PNG_NEXT).toString());
        
        Image imgAdd = new Image(MainController.class.getResource(Globals.PNG_ADD).toString());
        Image imgRevert = new Image(MainController.class.getResource(Globals.PNG_REVERT).toString());
        Image imgSaveAs = new Image(MainController.class.getResource(Globals.PNG_SAVE_AS).toString());
        
        if (btnPlay != null) {
            HelperFunctions.setupImage(btnPlay, imgPlay, size);
            btnPlay.setOnAction(e -> MediaPlayerControl.start(table, tableData, slSeek, slVolume));
        }
        if (btnStop != null) {
            HelperFunctions.setupImage(btnStop, imgStop, size);
            btnStop.setOnAction(e -> MediaPlayerControl.stop());
        }
        if (btnPause != null) {
            HelperFunctions.setupImage(btnPause, imgPause, size);
            btnPause.setOnAction(e -> MediaPlayerControl.pause());
        }
        if (btnPrev != null) {
            HelperFunctions.setupImage(btnPrev, imgPrev, size);
            btnPrev.setOnAction(e -> MediaPlayerControl.previousTrack(table, tableData, slSeek, slVolume));
        }
        if (btnNext != null) {
            HelperFunctions.setupImage(btnNext, imgNext, size);
            btnNext.setOnAction(e -> MediaPlayerControl.nextTrack(table, tableData, slSeek, slVolume));
        }
        if (btnRename != null) {
            HelperFunctions.setupImage(btnRename, imgAdd, size);
            btnRename.setOnAction(e -> rename());
        }
        if (btnRevert != null) {
            HelperFunctions.setupImage(btnRevert, imgRevert, size);
            btnRevert.setOnAction(e -> revert());
        }
        if (btnSaveAs != null) {
            HelperFunctions.setupImage(btnSaveAs, imgSaveAs, size);
            btnSaveAs.setOnAction(e -> saveAs(bundle));
        }
        if (slVolume != null) {
            slVolume.setMin(0);
            slVolume.setMax(1);
            String volStr = Globals.propman.getProperty(Globals.VOLUME, "1.0");
            slVolume.setValue(Double.parseDouble(volStr));
            slVolume.valueProperty().addListener((obs, oldVal, newVal) -> MediaPlayerControl.setVolume(newVal.doubleValue()));
        }
        if (slSeek != null) {
            slSeek.setMin(0);
            slSeek.setMax(100);
            slSeek.setValue(0);
            
            slSeek.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (slSeek.isValueChanging()) {
                    MediaPlayerControl.seekStart();
                    double percentage = newVal.doubleValue();
                    MediaPlayerControl.performSeek(percentage);
                    double total = MediaPlayerControl.getTotalDurationSeconds();
                    double seekTime = (percentage / 100.0) * total;
                    _log.info("Seeking (drag) to: " + seekTime + " seconds");
                    MediaPlayerControl.seekEnd();
                }
            });
            
            slSeek.setOnMouseClicked(event -> {
                if (event.getButton().equals(MouseButton.PRIMARY)) {
                    double mouseX = event.getX();
                    double sliderWidth = slSeek.getWidth();
                    if (sliderWidth <= 0) {
                        return; // avoid divide-by-zero
                    }
                    double percentage = (mouseX / sliderWidth) * 100;
                    percentage = Math.max(0, Math.min(100, percentage));
                    
                    slSeek.setValue(percentage);
                    MediaPlayerControl.performSeek(percentage);
                    
                    double total = MediaPlayerControl.getTotalDurationSeconds();
                    double seekTime = (percentage / 100.0) * total;
                    _log.info("Seeking (click) to: " + seekTime + " seconds");
                }
            });
            
            slSeek.setOnScroll(event -> {
                double total = MediaPlayerControl.getTotalDurationSeconds();
                if (total <= 0) {
                    return;
                }
                
                double deltaY = event.getDeltaY();
                double increment = (deltaY > 0 ? 1 : -1) * 1.0; // 1% per notch
                double current = slSeek.getValue();
                double newPct = Math.max(0, Math.min(100, current + increment));
                
                slSeek.setValue(newPct);
                MediaPlayerControl.performSeek(newPct);
                
                double seekTime = (newPct / 100.0) * total;
                _log.info("Seeking (wheel) to: " + seekTime + " seconds");
                
                event.consume();
            });
        }
    }
    
    private void initTable(ResourceBundle bundle) {
        TableColumn<AudioFile, Number> colIdx = new TableColumn<>("Idx");
        colIdx.setCellValueFactory(cellData -> cellData.getValue().idxProperty());
        TableColumn<AudioFile, String> colPrefix = new TableColumn<>(bundle.getString("col.prefix"));
        colPrefix.setCellValueFactory(cellData -> cellData.getValue().prefixProperty());
        HelperFunctions.centerColumn(colPrefix);
        TableColumn<AudioFile, String> colFileName = new TableColumn<>(bundle.getString("col.filename"));
        colFileName.setCellValueFactory(cellData -> cellData.getValue().fileNameProperty());
        TableColumn<AudioFile, String> colDuration = new TableColumn<>(bundle.getString("col.duration"));
        colDuration.setCellValueFactory(cellData -> cellData.getValue().durationProperty());
        colDuration.setCellFactory(column -> new TableCell<AudioFile, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    return;
                }
                AudioFile audioFile = getTableRow().getItem();
                setText(audioFile.durationProperty().get());
                setAlignment(Pos.CENTER);
                // Only trigger duration calc if not already done and file exists
                if (!audioFile.isDurationCalculated() && audioFile.getFile().exists()) {
                    calculateDuration(audioFile);
                }
            }
        });
        table.getColumns().addAll(colPrefix, colFileName, colDuration);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Resize columns with percentage widths (total must sum to 100%)
        double[] columnWidths = new double[]{10.0, 80.0, 10.0};

        // Defer resizing until TableView has a valid width
        if (table.getWidth() <= 0) {
            ChangeListener<Number> initialWidthListener = new ChangeListener<>() {
                @Override
                public void changed(javafx.beans.value.ObservableValue<? extends Number> obs, Number oldWidth, Number newWidth) {
                    if (newWidth != null && newWidth.doubleValue() > 0) {
                        HelperFunctions.resizeColumns(borderPane, table, columnWidths);
                        table.widthProperty().removeListener(this); // Remove after first valid resize
                    }
                }
            };
            table.widthProperty().addListener(initialWidthListener);
        } else {
            HelperFunctions.resizeColumns(borderPane, table, columnWidths);
        }
        
        table.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                AudioFile selectedAudio = table.getSelectionModel().getSelectedItem();
                if (selectedAudio != null) {
                    MediaPlayerControl.start(table, tableData, slSeek, slVolume);
                    _log.info("Double-clicked to play: " + selectedAudio.getFileName());
                }
            }
        });

        // Adjust column widths when TableView width changes
        table.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (newWidth != null && newWidth.doubleValue() > 0) {
                HelperFunctions.resizeColumns(borderPane, table, columnWidths);
            }
        });
        
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                lbDur.setText(selected.durationProperty().get());
            }
        });
        
        String last = Globals.propman.getProperty(Globals.OPEN_DIR_PATH, System.getProperty("user.dir"));
        File file = new File(last);
        if (file.isDirectory()) {
            loadFiles(file);
        }
    }
    
    private void calculateDuration(AudioFile audioFile) {
        if (audioFile.isDurationCalculated() || !audioFile.getFile().exists()) {
            return;
        }

        // Prevent duplicate tasks
        if (durationThreads.containsKey(audioFile)) {
            return;
        }
        
        CountDownLatch latch = new CountDownLatch(1);
        durationLatches.put(audioFile, latch);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                String uri = audioFile.getFile().toURI().toString();
                Media media = new Media(uri);
                MediaPlayer player = new MediaPlayer(media);
                try {
                    player.setOnReady(() -> {
                        try {
                            double secs = player.getTotalDuration().toSeconds();
                            if (Double.isNaN(secs)) {
                                secs = 0;
                            }
                            int min = (int) (secs / 60);
                            int sec = (int) (secs % 60);
                            String dur = String.format("%02d:%02d", min, sec);
                            
                            Platform.runLater(() -> {
                                audioFile.setDuration(dur);
                                audioFile.setDurationCalculated(true);
                                table.refresh();
                            });
                        } finally {
                            latch.countDown();
                            player.dispose();
                        }
                    });
                    
                    player.setOnError(() -> {
                        _log.error("Media error for {}: {}", audioFile.getFileName(), player.getError().getMessage());
                        Platform.runLater(() -> {
                            audioFile.setDuration("00:00");
                            audioFile.setDurationCalculated(true);
                            table.refresh();
                        });
                        latch.countDown();
                        player.dispose();
                    });

                    // Timeout after 10 seconds
                    if (!latch.await(10, TimeUnit.SECONDS)) {
                        Platform.runLater(() -> {
                            audioFile.setDuration("??:??");
                            audioFile.setDurationCalculated(true);
                            table.refresh();
                        });
                    }
                } catch (Exception e) {
                    _log.error("Failed to load media for duration: {}", audioFile.getFileName(), e);
                    Platform.runLater(() -> {
                        audioFile.setDuration("error");
                        audioFile.setDurationCalculated(true);
                        table.refresh();
                    });
                } finally {
                    if (player.getStatus() != MediaPlayer.Status.DISPOSED) {
                        player.dispose();
                    }
                    durationLatches.remove(audioFile);
                }
                return null;
            }
        };
        
        Thread thread = new Thread(task, "DurationCalc-" + audioFile.getFileName());
        durationThreads.put(audioFile, thread);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void openDir(ResourceBundle bundle) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(bundle.getString("dlg.open.title"));
        directoryChooser.setInitialDirectory(new File(Globals.propman.getProperty(Globals.OPEN_DIR_PATH, System.getProperty("user.home"))));
        File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            MediaPlayerControl.stop();
            loadFiles(selectedDirectory);
            Globals.propman.setProperty(Globals.OPEN_DIR_PATH, selectedDirectory.getAbsolutePath());
            Globals.propman.save();
        }
    }
    
    private void loadFiles(File baseDir) {
        List<AudioFile> list = new ArrayList<>();
        // Thread-safe counter for unique indexing
        AtomicInteger counter = new AtomicInteger(0);
        
        scanDirectoryRecursively(baseDir, list, counter);
        
        tableData = FXCollections.observableArrayList(list);
        table.setItems(tableData);
    }
    
    private void scanDirectoryRecursively(File dir, List<AudioFile> list, AtomicInteger counter) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        // Sort files for consistent ordering
        Arrays.sort(files, Comparator.comparing(File::isDirectory).thenComparing(File::getName));
        
        for (File file : files) {
            if (file.isDirectory()) {
                // Recursively scan subdirectories
                scanDirectoryRecursively(file, list, counter);
            } else if (isAudioFile(file)) {
                // Only add supported audio files
                int index = counter.getAndIncrement();
                AudioFile audioFile = new AudioFile(index, generatePrefix(index), file.getName(), file);
                list.add(audioFile);
            }
        }
    }
    
    private boolean isAudioFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3")
                || name.endsWith(".wav")
                || name.endsWith(".flac")
                || name.endsWith(".m4a")
                || name.endsWith(".aac")
                || name.endsWith(".ogg")
                || name.endsWith(".wma");
    }
    
    private String generatePrefix(int i) {
        return String.format("%0" + prefixLength + "d", i);
    }
    
    private void rename() {
        if (!tableData.isEmpty()) {
            renameFiles(table.getItems(), false);
        }
    }
    
    private void revert() {
        if (!tableData.isEmpty()) {
            renameFiles(table.getItems(), true);
        }
    }
    
    private void saveAs(ResourceBundle bundle) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle(bundle.getString("dlg.save.title"));
        File initialDir = new File(Globals.propman.getProperty(Globals.SAVE_AS_DIR_PATH, System.getProperty("user.home")));
        if (initialDir.isDirectory()) {
            dirChooser.setInitialDirectory(initialDir);
        }
        
        File targetDir = dirChooser.showDialog(stage);
        if (targetDir == null) {
            return; // User cancelled
        }
        // Save last used dir
        Globals.propman.setProperty(Globals.SAVE_AS_DIR_PATH, targetDir.getAbsolutePath());
        Globals.propman.save();

        // Get all items
        List<AudioFile> items = new ArrayList<>(table.getItems());
        if (items.isEmpty()) {
            showInformation(bundle.getString("msg.info"), bundle.getString("msg.empty"));
            return;
        }

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(bundle, stage, items.size());
        progressDialog.show();

        // Background task
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i < items.size(); i++) {
                    if (isCancelled()) {
                        break;
                    }
                    
                    AudioFile audio = items.get(i);
                    File src = audio.getFile();
                    String prefix = generatePrefix(i + 1);
                    String newName = prefix + "." + src.getName();
                    File dest = new File(targetDir, newName);
                    
                    try {
                        Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        updateProgress(i + 1, items.size());
                        updateMessage(bundle.getString("msg.copy") + ": " + newName);
                    } catch (IOException e) {
                        Platform.runLater(() -> showError(bundle.getString("msg.copy.fail"), src.getName() + "\n" + e.getMessage()));
                    }
                }
                return null;
            }
            
            @Override
            protected void succeeded() {
                progressDialog.close();
                showSuccess(bundle.getString("msg.save.ok"), items.size() + " " + bundle.getString("msg.save.to") + ":\n" + targetDir);
            }
            
            @Override
            protected void failed() {
                progressDialog.close();
                showError(bundle.getString("msg.save.fail"), getException().getMessage());
            }
            
            @Override
            protected void cancelled() {
                progressDialog.close();
                showWarning(bundle.getString("msg.warn"), "Save cancelled by user.");
            }
        };
        
        progressDialog.progressProperty().bind(saveTask.progressProperty());
        progressDialog.messageProperty().bind(saveTask.messageProperty());
        progressDialog.setOnCancel(() -> saveTask.cancel());
        
        new Thread(saveTask).start();
    }
    
    private void showInformation(String header, String content) {
        showCenteredAlert(Alert.AlertType.INFORMATION, header, content);
    }
    
    private void showSuccess(String header, String content) {
        showCenteredAlert(Alert.AlertType.INFORMATION, header, content);
    }
    
    private void showWarning(String header, String content) {
        showCenteredAlert(Alert.AlertType.WARNING, header, content);
    }
    
    private void showError(String header, String content) {
        showCenteredAlert(Alert.AlertType.ERROR, header, content);
    }
    
    private void showCenteredAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.initOwner(stage);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        alert.getDialogPane().getStylesheets().add(Globals.CSS_PATH);
        
        alert.showAndWait();
    }
    
    private void renameFiles(ObservableList<AudioFile> items, boolean revert) {
        for (AudioFile af : items) {
            Path oldPath = af.getFile().toPath();
            
            String oldName = af.getFileName();
            Path parent = oldPath.getParent();
            if (parent == null) {
                System.err.println("SKIP (no parent): " + oldName);
                continue;
            }
            
            String newName;
            if (revert) {
                newName = startsWithDigitPattern(oldName) ? oldName.substring(prefixLength + 1) : oldName;
            } else {
                String prefix = af.getPrefix().trim();
                newName = startsWithDigitPattern(oldName)
                        ? prefix + "." + oldName.substring(prefixLength + 1)
                        : prefix + "." + oldName;
            }
            
            if (oldName.equals(newName)) {
                System.out.println("NO-OP (same name): " + oldName);
                continue;
            }
            
            Path newPath = parent.resolve(newName);
            if (Files.exists(newPath)) {
                newName = newPath.getFileName().toString();
            }
            
            for (int i = 0; i < 1; i++) {
                System.out.println("RENAMED: " + oldName + " to " + newName);
                af.setFileName(newName);
                af.setDurationCalculated(false); // Force recalc
            }
        }
    }
    
    private boolean startsWithDigitPattern(String filename) {
        if (filename == null || filename.length() < 4) {
            return false; // Need at least "000."
        }

        // Check first 3 chars are digits
        for (int i = 0; i < prefixLength; i++) {
            if (!Character.isDigit(filename.charAt(i))) {
                return false;
            }
        }

        // Check 4th char is a dot
        return filename.charAt(prefixLength) == '.';
    }
    
    private Image createOpaqueDragView(ObservableList<AudioFile> items) {
        int count = items.size();
        double width = 200;
        double height = 20 + ((count <= 5 ? count : 5) * 25); // Header + rows

        // Create a clean canvas
        WritableImage image = new WritableImage((int) width, (int) height);
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Solid background
        gc.setFill(Color.web("#2c2c2c"));
        gc.fillRoundRect(0, 0, width, height, 12, 12);

        // Glowing border
        gc.setStroke(Color.web("#0078d4"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(1, 1, width - 2, height - 2, 10, 10);

        // Header: "Moving X files"
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 13));
        gc.fillText("Moving " + items.size() + " file" + (items.size() > 1 ? "s" : ""), 12, 16);

        // File names
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("System", 11));
        for (int i = 0; i < 5; i++) {
            if (i < items.size()) {
                String name = items.get(i).getFileName();
                if (name.length() > 28) {
                    name = name.substring(0, 25) + "...";
                }
                gc.fillText("▶ " + name, 12, 38 + (i * 18));
            }
        }

        // Render canvas to image with FULL OPAQUE pixels
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, image);
    }
    
    private void setupDragAndDrop() {
        table.setRowFactory(tv -> {
            TableRow<AudioFile> row = new TableRow<>();
            
            row.setOnDragDetected(event -> {
                if (row.isEmpty()) {
                    return;
                }
                
                ObservableList<AudioFile> selectedItems = table.getSelectionModel().getSelectedItems();
                if (selectedItems.isEmpty()) {
                    return;
                }
                
                Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.put(SERIALIZED_MIME_TYPE, row.getIndex());
                db.setContent(content);
                
                Image dragImage = createOpaqueDragView(selectedItems);
                db.setDragView(dragImage, event.getX(), event.getY());

                // Start auto-scroll monitoring
                startAutoScrollMonitoring(event.getSceneY());
                dragY = event.getSceneY();
                
                event.consume();
            });
            
            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    event.acceptTransferModes(TransferMode.MOVE);

                    // Update drag position for auto-scroll
                    dragY = event.getSceneY();
                    event.consume();
                }
            });
            
            row.setOnDragDropped(event -> {
                stopAutoScroll(); // Always stop scrolling on drop

                Dragboard db = event.getDragboard();
                if (!db.hasContent(SERIALIZED_MIME_TYPE)) {
                    event.setDropCompleted(false);
                    event.consume();
                    return;
                }
                
                int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                TableRow<AudioFile> dropRow = (TableRow<AudioFile>) event.getGestureTarget();
                int rawDropIndex = dropRow.isEmpty() ? tableData.size() : dropRow.getIndex();
                
                ObservableList<AudioFile> selected = FXCollections.observableArrayList(
                        table.getSelectionModel().getSelectedItems()
                );

                // Remove selected items
                tableData.removeAll(selected);

                // Adjust insert index
                int insertAt = rawDropIndex;
                if (draggedIndex < rawDropIndex) {
                    insertAt = Math.max(0, rawDropIndex - selected.size());
                }

                // Insert at new position
                tableData.addAll(insertAt, selected);

                // Update prefixes
                for (int i = 0; i < tableData.size(); i++) {
                    tableData.get(i).setPrefix(generatePrefix(i));
                }

                // Restore selection
                table.getSelectionModel().clearSelection();
                selected.forEach(table.getSelectionModel()::select);
                
                event.setDropCompleted(true);
                event.consume();
            });
            
            row.setOnDragDone(event -> {
                stopAutoScroll();
                event.consume();
            });
            
            return row;
        });
    }
    
    private void startAutoScrollMonitoring(double initialY) {
        stopAutoScroll(); // Safety

        autoScrollTimeline = new Timeline(new KeyFrame(
                SCROLL_INTERVAL,
                ae -> performAutoScroll()
        ));
        autoScrollTimeline.setCycleCount(Timeline.INDEFINITE);
        autoScrollTimeline.play();
    }
    
    private void performAutoScroll() {
        if (dragY <= 0) {
            return;
        }
        
        ScrollBar verticalScrollBar = getVerticalScrollbar();
        if (verticalScrollBar == null || !verticalScrollBar.isVisible()) {
            return;
        }
        
        Bounds tableBounds = table.localToScene(table.getBoundsInLocal());
        double topEdge = tableBounds.getMinY();
        double bottomEdge = tableBounds.getMaxY();
        
        double scrollDelta = 0;
        
        if (dragY < topEdge + SCROLL_THRESHOLD) {
            scrollDelta = -SCROLL_SPEED;
        } else if (dragY > bottomEdge - SCROLL_THRESHOLD) {
            scrollDelta = SCROLL_SPEED;
        }
        
        if (scrollDelta != 0) {
            double newValue = verticalScrollBar.getValue()
                    + scrollDelta / (verticalScrollBar.getMax() - verticalScrollBar.getMin());
            verticalScrollBar.setValue(
                    Math.max(0, Math.min(1, newValue))
            );
        }
    }
    
    private void stopAutoScroll() {
        if (autoScrollTimeline != null) {
            autoScrollTimeline.stop();
            autoScrollTimeline = null;
        }
    }
    
    private ScrollBar getVerticalScrollbar() {
        for (Node node : table.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar scrollBar
                    && scrollBar.getOrientation() == Orientation.VERTICAL) {
                return scrollBar;
            }
        }
        return null;
    }
    
    public double getVolume() {
        return slVolume.getValue();
    }
    
    public Stage getStage() {
        return stage;
    }
}
