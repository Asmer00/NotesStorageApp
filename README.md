# Notes Storage App — MADT Lab #5

An Android application demonstrating **Activity Interactions and Local Storage Persistence** in modern Android development. The application features complete CRUD operations, dual-storage engines (primary relational SQLite database and secondary internal JSON flat-file storage), one-way data synchronization, dynamic storage-type filtering, robust input validation, and Material 3 design principles.

---

## Table of Contents

1. [Overview & Educational Purpose](#overview--educational-purpose)
2. [Key Features](#key-features)
3. [Architecture Overview](#architecture-overview)
4. [CRUD Operations](#crud-operations)
5. [Dual Storage Engines](#dual-storage-engines)
   - [SQLite Storage (Primary)](#sqlite-storage-primary)
   - [JSON Storage (Secondary)](#json-storage-secondary)
   - [Storage Comparison](#storage-comparison)
6. [Storage Method Selection & Migration](#storage-method-selection--migration)
7. [SQLite-to-JSON Synchronization](#sqlite-to-json-synchronization)
8. [Input Validation & User Safeguards](#input-validation--user-safeguards)
9. [Project Structure](#project-structure)
10. [Opening and Running in Android Studio](#opening-and-running-in-android-studio)
11. [Running Unit Tests](#running-unit-tests)

---

## Overview & Educational Purpose

Developed as part of **MADT (Mobile Application Development Technologies) Lab #5**, this project explores core Android concepts:
- **Activity Interactions**: Navigating between screens and exchanging structured data via Intents and modern `ActivityResultLauncher` contracts (`registerForActivityResult`).
- **Data Persistence**: Implementing two distinct storage paradigms natively without third-party ORMs:
  - Structured, relational persistence using Android's native `SQLiteOpenHelper`.
  - Document-style flat-file persistence using internal file I/O and `org.json`.
- **Separation of Concerns**: Employing a Repository design pattern (`NoteRepository`) that decouples UI components from persistence logic.
- **Resource Management**: 100% externalized strings (`res/values/strings.xml`) for zero hardcoded UI strings.

---

## Key Features

- **Full CRUD Support**: Create, browse, inspect, edit, and delete notes effortlessly.
- **Dual-Storage Engine Architecture**:
  - **SQLite (Primary)**: Stores records in an on-device relational SQLite database (`notes_app.db`).
  - **JSON (Secondary)**: Persists records in internal app storage as a formatted JSON document (`notes.json`).
- **Interactive Storage Engine Selector**: Choose the target storage engine per note using radio buttons when creating or editing.
- **Safe Storage Migration**: Moving a note from SQLite to JSON (or vice versa) saves to the new target first before removing it from the old engine, preventing data loss.
- **Tab Filtering**: Quick navigation via `TabLayout` to filter notes by storage type (`All`, `SQLite`, or `JSON`).
- **Visual Storage Badges**: Color-coded badges on note cards distinguish SQLite notes (green) from JSON notes (blue) at a glance.
- **One-Way Synchronization**: Sync SQLite notes into JSON storage with a single tap, updating existing entries while preserving JSON-only notes.
- **Comprehensive Validation**: Prevents blank titles and content with real-time Material `TextInputLayout` error hints and text-watcher cleanup.
- **User Safety Dialogs**: Confirmation dialogs for note deletion and protection against accidental data loss when exiting an edited note with unsaved changes.
- **Modern UI & View Binding**: Built with Material Design 3 components, `CoordinatorLayout`, `RecyclerView` with `DiffUtil`, and View Binding for type-safe view access.

---

## Architecture Overview

The app follows a clean, decoupled layer architecture:

```
┌────────────────────────────────────────────────────────┐
│                        UI Layer                        │
│   MainActivity (List, Tabs, Sync)                      │
│   NoteEditorActivity (Create/Edit Form, Validation)    │
│   NoteAdapter (RecyclerView, DiffUtil, Badges)         │
└──────────────────────────┬─────────────────────────────┘
                           │ Calls methods on
                           ▼
┌────────────────────────────────────────────────────────┐
│                   Repository Layer                     │
│   NoteRepository (Singleton, Single Source of Truth)   │
└──────────────┬───────────────────────────┬─────────────┘
               │ Delegates to              │ Delegates to
               ▼                           ▼
┌───────────────────────────────┐ ┌───────────────────────┐
│        SQLite Storage         │ │     JSON Storage      │
│  SqliteNoteStorage            │ │  JsonNoteStorage      │
│  NotesDbHelper (notes_app.db) │ │  notes.json (filesDir)│
└───────────────────────────────┘ └───────────────────────┘
```

- **Model Layer** (`model/`): Contains the immutable `Note` data class and `StorageType` enum.
- **Storage Layer** (`storage/`): Defines the `NoteStorage` contract interface implemented by `SqliteNoteStorage` and `JsonNoteStorage`. Managed via `NoteRepository`.
- **UI Layer** (`ui/`): Contains activities, adapter, and view-binding logic. Activities never query databases or files directly.

---

## CRUD Operations

The application provides a complete implementation of CRUD (Create, Read, Update, Delete) workflows:

### 1. Create (C)
- **UI Trigger**: Tap the `+` Floating Action Button (`fabAddNote`) in `MainActivity`.
- **Flow**:
  1. `MainActivity` launches `NoteEditorActivity` via `ActivityResultLauncher` (`editorLauncher`).
  2. The user enters a title and content, and selects the storage destination (SQLite by default).
  3. Clicking **Save Note** (or the toolbar save checkmark) validates the input.
  4. `NoteRepository.saveNewNote(title, content, targetStorage)` writes to the selected engine.
  5. `NoteEditorActivity` finishes with `RESULT_OK` and `ACTION_TYPE_SAVED`.
  6. `MainActivity` refreshes the list and displays a `"Note saved successfully"` Snackbar.

### 2. Read / View (R)
- **UI Presentation**:
  - Notes are displayed in a `RecyclerView` using card components (`item_note.xml`).
  - Each card displays the note **Title**, a multi-line **Content preview** (up to 3 lines), a formatted **Timestamp** (`MMM d, yyyy, HH:mm`), and a color-coded **Storage Badge** (`SQLite` or `JSON`).
- **Filtering**:
  - The top `TabLayout` allows filtering the list by `All` (both engines, sorted by timestamp descending), `SQLite` only, or `JSON` only.
- **Empty State**:
  - When no notes exist for the selected tab, a clean placeholder view (`layoutEmptyState`) displays an empty note icon, title, and prompt.

### 3. Update / Edit (U)
- **UI Trigger**: Tap any note card in `MainActivity`.
- **Flow**:
  1. `NoteEditorActivity` launches in edit mode, pre-populating the title, content, and the storage radio button corresponding to the note's current engine.
  2. Modifying text updates the note in its existing engine via `NoteRepository.updateNote()`.
  3. Switching the radio button triggers a **cross-storage migration** (see [Storage Method Selection & Migration](#storage-method-selection--migration)).
  4. Returns `RESULT_OK` with `ACTION_TYPE_UPDATED`, prompting a `"Note updated successfully"` Snackbar.
- **Unsaved Changes Guard**:
  - If the user edits any field and taps the system Back button or the Up navigation arrow, a confirmation dialog warns: *"You have unsaved changes. Are you sure you want to exit without saving?"* with **Discard** and **Keep Editing** actions.

### 4. Delete (D)
- **UI Triggers**:
  - **Quick Delete**: Tap the trash icon (`btnDeleteNote`) on any note card in `MainActivity`.
  - **Editor Delete**: Tap the trash icon in the toolbar menu of `NoteEditorActivity` (visible only in edit mode).
- **Safety Prompt**:
  - Both triggers present an `AlertDialog` confirming: *"Are you sure you want to delete note \"[Title]\"?"*
- **Execution**:
  - Upon confirmation, `NoteRepository.deleteNote(note)` deletes the record from its active storage engine.
  - Returns `RESULT_OK` with `ACTION_TYPE_DELETED` and shows a `"Note deleted"` Snackbar.

---

## Dual Storage Engines

### SQLite Storage (Primary)

- **Class**: `SqliteNoteStorage.kt` backed by `NotesDbHelper.kt`.
- **Technology**: Native Android SQLite framework (`SQLiteOpenHelper`, `SQLiteDatabase`, `ContentValues`, `Cursor`). Zero third-party dependencies.
- **Database File**: `notes_app.db` (Version 1).
- **Location on Device**: `/data/data/com.example.notesstorageapp/databases/notes_app.db`
- **Table Schema**:
  ```sql
  CREATE TABLE notes (
      _id INTEGER PRIMARY KEY AUTOINCREMENT,
      title TEXT NOT NULL,
      content TEXT NOT NULL,
      timestamp INTEGER NOT NULL
  )
  ```
- **Key Characteristics**:
  - Parameterized queries (`selectionArgs`) prevent SQL injection attacks.
  - Queries return records ordered by newest first: `timestamp DESC`.
  - Cursor resources are safely auto-closed using Kotlin's `.use { ... }` extension.

### JSON Storage (Secondary)

- **Class**: `JsonNoteStorage.kt`.
- **Technology**: Native Android `org.json` package (`JSONArray`, `JSONObject`) and standard Java File I/O (`FileInputStream`, `FileOutputStream`, `InputStreamReader`, `OutputStreamWriter`).
- **File Name**: `notes.json`.
- **Location on Device**: `/data/data/com.example.notesstorageapp/files/notes.json` (`context.filesDir/notes.json`).
- **Data Structure**:
  ```json
  [
    {
      "id": 1731964200000,
      "title": "Grocery List",
      "content": "Milk, Eggs, Bread",
      "timestamp": 1731964200000
    }
  ]
  ```
- **Concurrency & Thread Safety**:
  - All read, write, update, and delete routines are protected by a synchronized file lock (`synchronized(FILE_LOCK)`), preventing data corruption or race conditions.
- **Monotonic Unique ID Generation**:
  - Unlike SQLite's `AUTOINCREMENT`, JSON entries require unique ID generation.
  - `JsonNoteStorage` uses a monotonic ID generator:
    ```kotlin
    val maxExistingId = notes.maxOfOrNull { it.id } ?: 0L
    val now = System.currentTimeMillis()
    val newId = if (now > maxExistingId) now else maxExistingId + 1L
    ```
    This guarantees strictly unique, ascending IDs even if multiple notes are generated in the same millisecond.

### Storage Comparison

| Dimension | SQLite (Primary) | JSON (Secondary) |
| :--- | :--- | :--- |
| **Storage Type** | Relational database (`.db`) | Flat JSON text file (`.json`) |
| **Android API** | `SQLiteOpenHelper`, `Cursor` | `org.json`, `context.filesDir` |
| **ID Strategy** | Native `AUTOINCREMENT` | Monotonic timestamp algorithm |
| **Performance Profile** | Fast indexed queries; efficient for large datasets | Lightweight document format; simple export/import |
| **Badge Color** | Green (`#2E7D32` text, `#E8F5E9` bg) | Blue (`#1565C0` text, `#E3F2FD` bg) |

---

## Storage Method Selection & Migration

### How Users Select a Storage Method

Inside `NoteEditorActivity`, a dedicated **"Save destination:"** card presents two radio options:
1. **SQLite Database (Primary)** (`rbStorageSqlite`) — Selected by default for newly created notes.
2. **JSON File Storage (Secondary)** (`rbStorageJson`).

The user can choose either destination prior to saving.

### Safe Cross-Storage Migration

When editing an existing note, the radio group automatically reflects where the note is currently stored. If the user selects the opposite engine and taps **Save**:

```
[User Changes Radio from SQLite to JSON]
                 │
                 ▼
1. Save note to JSON engine first:
   NoteRepository.saveNewNote(title, content, StorageType.JSON)
                 │
                 ▼ (Only after save succeeds)
2. Delete note from SQLite engine:
   NoteRepository.deleteNote(existingSqliteNote)
```

By executing the insertion into the target engine **before** deleting from the original engine, the app eliminates the risk of data loss if the write operation fails.

---

## SQLite-to-JSON Synchronization

The app includes a built-in synchronization utility that copies notes from primary SQLite storage into secondary JSON storage.

### How to Trigger Synchronization
- Tap the **Sync** icon (`ic_sync`) in the top-right corner of the `MainActivity` AppBar.

### Merge & Upsert Strategy
The sync routine (`NoteRepository.syncSqliteToJson()`) executes the following steps:
1. Queries all records currently present in the SQLite database.
2. Reads existing records from `notes.json` into a lookup map indexed by `id`.
3. For each SQLite note:
   - If a note with the same `id` exists in JSON, its title, content, and timestamp are updated with the SQLite values.
   - If the `id` does not exist in JSON, it is inserted as a new record.
4. **Preserves JSON-Only Notes**: Any notes that were created directly in JSON storage and do not exist in SQLite are left intact.
5. Writes the merged list back to `notes.json` atomically.

### User Feedback
- **Success with updates**: Displays a Snackbar: `Synced X note(s) to JSON storage`.
- **Empty SQLite database**: Displays a Snackbar: `No notes in SQLite to synchronize`.
- **Immediate UI Update**: Automatically reloads notes so the newly synced entries appear immediately in the `JSON` and `All` tabs.

---

## Input Validation & User Safeguards

### Real-Time Validation
- **Rules**: Both **Title** and **Content** are mandatory fields. Inputs containing only whitespace are rejected (`trim().isEmpty()`).
- **Visual Feedback**:
  - The input boxes use Material 3 `TextInputLayout` containers with `errorEnabled="true"`.
  - When saving with an empty field, the container displays an inline error message:
    - Title: `"Title cannot be empty"`
    - Content: `"Content cannot be empty"`
  - A summary warning Snackbar appears: `"Please provide both title and content"`.
- **Reactive Clearing**:
  - Input fields use `doAfterTextChanged` watchers. As soon as the user enters valid non-whitespace text, the error indicators are cleared automatically.

### Confirmation Dialogs
- **Delete Confirmation**: Every deletion (via card button or editor menu) requires confirmation through an `AlertDialog` displaying the specific note title.
- **Discard Unsaved Changes**: When backing out of `NoteEditorActivity`, if any edits were made to the title, content, or storage engine, the user is alerted to confirm discarding changes.

---

## Project Structure

```
NotesStorageApp/
├── app/
│   ├── build.gradle.kts                   # App-level Gradle build file (SDK 34, ViewBinding, dependencies)
│   ├── proguard-rules.pro                 # ProGuard rules for release builds
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml        # Declares MainActivity & NoteEditorActivity
│       │   ├── java/com/example/notesstorageapp/
│       │   │   ├── model/
│       │   │   │   ├── Note.kt            # Immutable Note data class (Serializable)
│       │   │   │   └── StorageType.kt     # Enum: SQLITE, JSON
│       │   │   ├── storage/
│       │   │   │   ├── NoteStorage.kt     # Common interface for CRUD operations
│       │   │   │   ├── NoteRepository.kt  # Repository & singleton coordinator
│       │   │   │   ├── sqlite/
│       │   │   │   │   ├── NotesDbHelper.kt      # SQLiteOpenHelper & schema definition
│       │   │   │   │   └── SqliteNoteStorage.kt  # SQLite NoteStorage implementation
│       │   │   │   └── json/
│       │   │   │       └── JsonNoteStorage.kt    # JSON file NoteStorage implementation
│       │   │   └── ui/
│       │   │       ├── MainActivity.kt        # Note list, tabs, sync, empty state
│       │   │       ├── NoteAdapter.kt         # RecyclerView adapter with DiffUtil & badges
│       │   │       └── NoteEditorActivity.kt  # Editor screen: validation, storage radio group
│       │   └── res/
│       │       ├── drawable/                  # Vector drawables (icons & badge backgrounds)
│       │       ├── layout/
│       │       │   ├── activity_main.xml         # Main screen layout (AppBar, Tabs, RecyclerView)
│       │       │   ├── activity_note_editor.xml  # Editor layout (Inputs, RadioGroup, Button)
│       │       │   └── item_note.xml             # Note card layout (Title, snippet, badge, date)
│       │       ├── menu/
│       │       │   ├── menu_main.xml             # Main menu (Sync action)
│       │       │   └── menu_note_editor.xml      # Editor menu (Save & Delete actions)
│       │       └── values/
│       │           ├── colors.xml             # Theme and badge color definitions
│       │           ├── strings.xml            # 100% externalized string resources
│       │           └── themes.xml             # Material 3 light application theme
│       └── test/java/com/example/notesstorageapp/
│           └── NoteModelTest.kt               # Unit tests for Note model, migration & ID generation
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties              # Configured for Gradle 8.7
├── build.gradle.kts                           # Root Gradle build file (AGP 8.5.2, Kotlin 1.9.24)
├── settings.gradle.kts                        # Module definitions
├── gradle.properties                          # JVM memory & AndroidX flags
└── README.md                                  # Project documentation
```

---

## Opening and Running in Android Studio

### Prerequisites
- **Android Studio**: Android Studio Jellyfish (2023.3.1), Koala (2024.1.1), Ladybug (2024.2.1), or newer.
- **JDK**: JDK 17 or JDK 21 (tested and fully compatible with JDK 21).
- **Android SDK**: Android 14.0 (API Level 34) platform and build tools.
- **Device / Emulator**: Android Virtual Device (AVD) or physical device running Android 7.0 (API Level 24) or higher.

### Step-by-Step Instructions

1. **Launch Android Studio**.
2. Click **File > Open...** (or click **Open** on the welcome screen).
3. Browse to the project root directory:
   ```
   C:\Users\Administrator\AndroidStudioProjects\NotesStorageApp
   ```
   Click **OK**.
4. **Sync Project with Gradle Files**:
   - Android Studio will detect the included Gradle 8.7 wrapper and begin synchronizing dependencies.
   - If prompted to configure the Gradle JDK, navigate to:
     `Settings / Preferences > Build, Execution, Deployment > Build Tools > Gradle`
     and verify that **Gradle JDK** is set to **JDK 17** or **JDK 21**.
5. **Select a Target Device**:
   - In the top toolbar device selector, choose a running Android emulator or a connected physical device with USB debugging enabled.
6. **Build and Run**:
   - Click the green **Run** button (or press `Shift + F10`).
   - The app will compile, install, and launch `MainActivity`.

---

## Running Unit Tests

The test suite covers core business logic, note transformations, storage migration, and unique ID generation algorithms:

- `testNoteCreation`: Verifies field assignments and default parameters.
- `testNoteUpdateCopy`: Verifies immutability and copy behavior.
- `testNoteStorageMigration`: Confirms storage type transition logic.
- `testMonotonicUniqueIdGenerationLogic`: Validates that sequential ID generation remains strictly unique and monotonically increasing even when timestamps coincide.

### In Android Studio
1. In the **Project** window, navigate to:
   `app > src > test > java > com.example.notesstorageapp > NoteModelTest.kt`
2. Right-click `NoteModelTest.kt` and select **Run 'NoteModelTest'** (or press `Ctrl + Shift + F10`).
3. Results will be displayed in the **Run / Test** window.

### Via Command Line (Gradle Wrapper)
Open PowerShell or Terminal in the project root and execute:

```powershell
.\gradlew.bat test
```

*(On macOS / Linux: `./gradlew test`)*

All unit tests pass cleanly with zero warnings.
