package com.nc.sortabeat.pojo;

import java.io.File;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Neural Cortex
 */
public class AudioFile {

    private final SimpleIntegerProperty idx;
    private final SimpleStringProperty prefix;
    private final SimpleStringProperty fileName;
     private final SimpleStringProperty duration;
     private boolean durationCalculated; // New flag to track if duration is calculated
    private File file;

    public AudioFile(int idx, String prefix, String fileName,File file) {
        this.idx = new SimpleIntegerProperty(idx);
        this.prefix = new SimpleStringProperty(prefix);
        this.fileName = new SimpleStringProperty(fileName);
        this.file=file;
        this.duration=new SimpleStringProperty("00:00");
        this.durationCalculated = false; // Initially not calculated
    }

    public void setIdx(int idx) {
        this.idx.set(idx);
    }

    public int getIdx() {
        return this.idx.get();
    }

    public SimpleIntegerProperty idxProperty() {
        return this.idx;
    }

    public void setPrefix(String prefix) {
        this.prefix.set(prefix);
    }

    public String getPrefix() {
        return this.prefix.get();
    }

    public SimpleStringProperty prefixProperty() {
        return this.prefix;
    }

    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }

    public String getFileName() {
        return this.fileName.get();
    }

    public SimpleStringProperty fileNameProperty() {
        return this.fileName;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
    
    
    
   public SimpleStringProperty durationProperty() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration.set(duration);
        this.durationCalculated = true; // Mark as calculated
    }

    public boolean isDurationCalculated() {
        return durationCalculated;
    }

    public void setDurationCalculated(boolean durationCalculated) {
        this.durationCalculated = durationCalculated;
    }
    
    
}
