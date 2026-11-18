package com.example.notesstorageapp.storage

import android.content.Context
import com.example.notesstorageapp.model.Note
import com.example.notesstorageapp.model.StorageType
import com.example.notesstorageapp.storage.json.JsonNoteStorage
import com.example.notesstorageapp.storage.sqlite.SqliteNoteStorage

/**
 * Repository class that acts as the single source of truth for notes data.
 * Completely decouples storage and database operations from Activities.
 */
class NoteRepository private constructor(context: Context) {

    private val sqliteStorage = SqliteNoteStorage(context)
    private val jsonStorage = JsonNoteStorage(context)

    companion object {
        @Volatile
        private var instance: NoteRepository? = null

        fun getInstance(context: Context): NoteRepository {
            return instance ?: synchronized(this) {
                instance ?: NoteRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Retrieves notes according to the specified filter.
     * If storageType is null, returns notes from both SQLite and JSON, sorted by newest first.
     */
    fun getNotes(storageType: StorageType? = null): List<Note> {
        return when (storageType) {
            StorageType.SQLITE -> sqliteStorage.getAllNotes()
            StorageType.JSON -> jsonStorage.getAllNotes()
            null -> {
                val sqliteNotes = sqliteStorage.getAllNotes()
                val jsonNotes = jsonStorage.getAllNotes()
                (sqliteNotes + jsonNotes).sortedByDescending { it.timestamp }
            }
        }
    }

    /**
     * Saves a new note in the selected storage engine.
     */
    fun saveNewNote(title: String, content: String, targetStorage: StorageType): Note {
        return when (targetStorage) {
            StorageType.SQLITE -> sqliteStorage.insertNote(title, content)
            StorageType.JSON -> jsonStorage.insertNote(title, content)
        }
    }

    /**
     * Updates an existing note in its corresponding storage engine.
     */
    fun updateNote(note: Note): Boolean {
        return when (note.storageType) {
            StorageType.SQLITE -> sqliteStorage.updateNote(note)
            StorageType.JSON -> jsonStorage.updateNote(note)
        }
    }

    /**
     * Deletes a note from its corresponding storage engine.
     */
    fun deleteNote(note: Note): Boolean {
        return when (note.storageType) {
            StorageType.SQLITE -> sqliteStorage.deleteNote(note.id)
            StorageType.JSON -> jsonStorage.deleteNote(note.id)
        }
    }

    /**
     * Synchronizes notes from SQLite (primary) to JSON (secondary) storage.
     * Uses an upsert/merge strategy: updates existing matching notes and inserts new ones,
     * while preserving notes created exclusively in JSON storage.
     *
     * @return Number of notes synced from SQLite.
     */
    fun syncSqliteToJson(): Int {
        val sqliteNotes = sqliteStorage.getAllNotes()
        return jsonStorage.upsertNotesFromSqlite(sqliteNotes)
    }
}
