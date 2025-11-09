# SortaBeat

![Application Screenshot](https://github.com/NeuralCortex/SortaBeat/blob/main/app.png)

## Overview

**SortaBeat** is a lightweight **JavaFX-based MP3 organizer and player** that helps you **force a custom playback order** by adding numeric prefixes to file names (e.g., `001_Song.mp3`, `002_Next.mp3`).
This is essential for **car CD/MP3 players** that sort files alphabetically — ensuring your album or playlist plays in the **exact intended sequence**.


*SortaBeat — Because your car stereo shouldn’t DJ your road trip.*

## Features

- **Recursive folder scanning** (including subdirectories)
- **Drag & Drop reordering** with **multi-selection** (hold `Ctrl`)
- **Smart prefixing** – click **+** to renumber based on current order
- **Safe export** – copies renamed files to a new folder (originals untouched)
- **Real-time preview** of final filenames
- **Built-in audio playback**

---

## Requirements

| Component       | Minimum Version |
|-----------------|-----------------|
| **Java (JRE/JDK)** | **24** |
| **JavaFX SDK**     | **21.0.6** |

> **Note**: JavaFX is **not bundled** with JDK 11+. Download it separately from [Gluon](https://gluonhq.com/products/javafx/).

---

## How to Use (Workflow)

1. **Open a folder**  
   → All `.mp3` files (including subfolders) are loaded automatically.

2. **Rearrange tracks**  
   → Drag & drop to reorder (select multiple with **Ctrl**)

3. **Add prefixes**  
   → Click **+** to apply sequential numbers  
   → Reorder again? Click **+** again to **update prefixes**

4. **Export**  
   → Choose a destination folder  
   → Renamed copies are saved — **originals remain unchanged**

---

## Create Windows Installer (`setup.exe`) with Inno Setup

You can package **SortaBeat** into a professional **Windows installer** using **[Inno Setup](https://jrsoftware.org/isinfo.php)**.

### Steps

1. **Build the JAR with dependencies** (using NetBeans):
    → Output: `target/SortaBeat-1.0.0.jar`

2. **Edit the installer script**:
   - Open `SortaBeat.iss`
   - Adjust paths, version, app name, etc.
   - Ensure all paths match your build output

3. **Run the build script** using Inno Setup

4. **Output**:
   - `SortaBeat 1.0.0 Setup.exe`

> **Important**: The `.iss` and `.bat` files may need to be **customized** depending on:  
> - Your **JAR name/version**  
> - **JRE/JavaFX folder structure**  
> - **Installation directory** or **desktop shortcut** settings

---

## Technologies

| Tool              | Link |
|-------------------|------|
| **IDE**           | [Apache NetBeans 27](https://netbeans.apache.org/) |
| **Java SDK**      | [JDK 24](https://www.oracle.com/java/technologies/downloads/#jdk24-windows) |
| **JavaFX**        | [JavaFX ≥ 21.0.6](https://gluonhq.com/products/javafx/) |
| **GUI Design**    | [Gluon SceneBuilder](https://gluonhq.com/products/scene-builder/) *(optional)* |

> SceneBuilder is only needed for editing the UI — **not required to run the app**.