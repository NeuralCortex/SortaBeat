package com.nc.sortabeat.controller;

import com.nc.sortabeat.pojo.AudioFile;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Slider;
import javafx.scene.control.TableView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import javafx.scene.control.Label;

/**
 *
 * @author Neural Cortex
 */
public final class MediaPlayerControl {

    private static final Logger _log = LogManager.getLogger(MediaPlayerControl.class);
    private static final Object LOCK = new Object();

    private static MediaPlayer mediaPlayer;
    private static AudioFile currentAudioFile;
    private static boolean isSeeking = false;
    private static boolean autoPlayNext = true;

    private static Label lbNow;

    private MediaPlayerControl() {

    }

    public static void start(TableView<AudioFile> table, ObservableList<AudioFile> tableData, Slider slSeek, Slider slVolume) {
        synchronized (LOCK) {
            AudioFile selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                _log.warn("No audio file selected for playback.");
                return;
            }

            stop();

            File file = selected.getFile();
            if (!file.exists() || !file.isFile()) {
                _log.error("Invalid media file: " + file.getAbsolutePath());
                return;
            }

            try {
                Media media = new Media(file.toURI().toString());
                mediaPlayer = new MediaPlayer(media);
                currentAudioFile = selected;

                double volume = slVolume != null ? slVolume.getValue() : 1.0;
                mediaPlayer.setVolume(volume);

                mediaPlayer.setOnReady(() -> Platform.runLater(() -> onPlayerReady(table, tableData, slSeek, slVolume)));
                mediaPlayer.setOnError(() -> _log.error("MediaPlayer error: " + mediaPlayer.getError().getMessage()));
            } catch (Exception e) {
                _log.error("Error loading media: " + e.getMessage());
                mediaPlayer = null;
            }
        }
    }

    public static void stop() {
        MediaPlayer mp;
        synchronized (LOCK) {
            if (mediaPlayer == null) {
                return;
            }
            mp = mediaPlayer;
            mediaPlayer = null;
            currentAudioFile = null;
        }

        CountDownLatch done = new CountDownLatch(1);
        try {
            mp.stop();
            mp.dispose();
            _log.info("MP3 FREE – handle released");
        } finally {
            done.countDown();
        }
    }

    public static void pause() {
        synchronized (LOCK) {
            if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                _log.info("Playback paused.");
            }
        }
    }

    public static void setVolume(double volume) {
        synchronized (LOCK) {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(volume);
                _log.info("Volume set to: " + (volume * 100) + "%");
            }
        }
    }

    public static void previousTrack(TableView<AudioFile> table, ObservableList<AudioFile> tableData, Slider slSeek, Slider slVolume) {
        navigateTrack(table, tableData, slSeek, slVolume, -1);
    }

    public static void nextTrack(TableView<AudioFile> table, ObservableList<AudioFile> tableData, Slider slSeek, Slider slVolume) {
        navigateTrack(table, tableData, slSeek, slVolume, +1);
    }

    public static void toggleAutoPlayNext() {
        autoPlayNext = !autoPlayNext;
        _log.info("Auto-play-next " + (autoPlayNext ? "enabled" : "disabled"));
    }

    public static boolean isAutoPlayNext() {
        return autoPlayNext;
    }

    private static void onPlayerReady(TableView<AudioFile> table, ObservableList<AudioFile> tableData, Slider slSeek, Slider slVolume) {
        synchronized (LOCK) {
            if (mediaPlayer == null) {
                return;
            }

            if (slSeek != null) {
                slSeek.setValue(0);
                mediaPlayer.currentTimeProperty().addListener((obs, old, now) -> {
                    if (!isSeeking && !slSeek.isValueChanging() && mediaPlayer != null) {
                        double total = mediaPlayer.getTotalDuration().toSeconds();
                        if (total > 0) {
                            double progress = (now.toSeconds() / total) * 100;
                            slSeek.setValue(progress);
                        }
                    }
                });
            }

            mediaPlayer.setOnEndOfMedia(() -> {
                if (autoPlayNext) {
                    Platform.runLater(() -> playNextTrack(table, tableData, slSeek, slVolume));
                }
            });

            updateCurrentTimeLabel();

            mediaPlayer.play();
            _log.info("Playing: " + currentAudioFile.getFileName());
        }
    }

    private static void playNextTrack(TableView<AudioFile> table, ObservableList<AudioFile> tableData, Slider slSeek, Slider slVolume) {
        if (tableData == null || tableData.isEmpty()) {
            _log.warn("No tracks to play next.");
            return;
        }

        int cur = table.getSelectionModel().getSelectedIndex();
        int next = (cur + 1) % tableData.size();

        table.getSelectionModel().clearAndSelect(next);
        start(table, tableData, slSeek, slVolume); // volume unchanged
        _log.info("Auto-play next: " + tableData.get(next).getFileName());
    }

    private static void navigateTrack(TableView<AudioFile> table, ObservableList<AudioFile> tableData, Slider slSeek, Slider slVolume, int delta) {
        if (tableData == null || tableData.isEmpty()) {
            _log.warn("No tracks available.");
            return;
        }

        int cur = table.getSelectionModel().getSelectedIndex();
        int size = tableData.size();
        int target = (cur + delta + size) % size; // handles negative delta

        table.getSelectionModel().clearAndSelect(target);
        start(table, tableData, slSeek, slVolume);
        _log.info((delta < 0 ? "Previous" : "Next") + " track: " + tableData.get(target).getFileName());
    }

    public static void seekStart() {
        isSeeking = true;
    }

    public static void seekEnd() {
        isSeeking = false;
    }

    public static void performSeek(double percentage) {
        synchronized (LOCK) {
            if (mediaPlayer == null) {
                return;
            }
            double total = getTotalDurationSeconds();
            if (total <= 0) {
                return;
            }

            double clamped = Math.max(0, Math.min(100, percentage));
            double target = (clamped / 100.0) * total;
            mediaPlayer.seek(Duration.seconds(target));
            _log.debug("Seek to " + target + " s (" + clamped + "%)");
        }
    }

    private static void updateCurrentTimeLabel() {
        if (mediaPlayer == null) {
            lbNow.setText("00:00");
            return;
        }

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (newTime != null && !newTime.isUnknown()) {
                long minutes = (long) newTime.toMinutes();
                long seconds = (long) newTime.toSeconds() % 60;
                Platform.runLater(()
                        -> lbNow.setText(String.format("%02d:%02d", minutes, seconds))
                );
            }
        });

        Duration current = mediaPlayer.getCurrentTime();
        if (current != null && !current.isUnknown()) {
            long minutes = (long) current.toMinutes();
            long seconds = (long) current.toSeconds() % 60;
            lbNow.setText(String.format("%02d:%02d", minutes, seconds));
        }
    }

    public static double getTotalDurationSeconds() {
        synchronized (LOCK) {
            if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
                return mediaPlayer.getTotalDuration().toSeconds();
            }
            return 0.0;
        }
    }

    public static boolean nullCheck() {
        return mediaPlayer == null && currentAudioFile == null;
    }

    public static void setLbNow(Label lbNow) {
        MediaPlayerControl.lbNow = lbNow;
    }
}
