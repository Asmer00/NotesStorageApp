package com.example.notesstorageapp.storage.json

import android.content.Context
import com.example.notesstorageapp.model.Note
import com.example.notesstorageapp.model.StorageType
import com.example.notesstorageapp.storage.NoteStorage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Secondary storage implementation saving notes as a JSON file in internal application storage.
 * Uses Android's built-in org.json package with zero third-party dependencies.
 */
class JsonNoteStorage(private val context: Context) : NoteStorage {

    companion object {
        private const val FILE_NAME = "notes.json"
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_CONTENT = "content"
        private const val KEY_TIMESTAMP = "timestamp"

        // Lock object to synchronize file read/write operations
        private val FILE_LOCK = Any()
    }

    private val notesFile: File
        get() = File(context.filesDir, FILE_NAME)

    override fun getAllNotes(): List<Note> = synchronized(FILE_LOCK) {
        val notes = readNotesFromFile()
        return notes.sortedByDescending { it.timestamp }
    }

    override fun getNoteById(id: Long): Note? = synchronized(FILE_LOCK) {
        return readNotesFromFile().find { it.id == id }
    }

    override fun insertNote(title: String, content: String): Note = synchronized(FILE_LOCK) {
        val notes = readNotesFromFile().toMutableList()
        // Generate a guaranteed unique, monotonically increasing ID
        val maxExistingId = notes.maxOfOrNull { it.id } ?: 0L
        val now = System.currentTimeMillis()
        val newId = if (now > maxExistingId) now else maxExistingId + 1L

        val newNote = Note(
            id = newId,
            title = title,
            content = content,
            timestamp = now,
            storageType = StorageType.JSON
        )
        notes.add(newNote)
        writeNotesToFile(notes)
        return newNote
    }

    override fun updateNote(note: Note): Boolean = synchronized(FILE_LOCK) {
        val notes = readNotesFromFile().toMutableList()
        val index = notes.indexOfFirst { it.id == note.id }
        if (index != -1) {
            notes[index] = note.copy(
                timestamp = System.currentTimeMillis(),
                storageType = StorageType.JSON
            )
            return writeNotesToFile(notes)
        }
        return false
    }

    override fun deleteNote(id: Long): Boolean = synchronized(FILE_LOCK) {
        val notes = readNotesFromFile().toMutableList()
        val removed = notes.removeAll { it.id == id }
        if (removed) {
            return writeNotesToFile(notes)
        }
        return false
    }

    /**
     * Upserts a collection of notes into JSON storage (used for synchronization).
     * If a note with matching ID exists, updates its content; otherwise appends it.
     * Notes already existing only in JSON are preserved.
     *
     * @return The count of notes synced from SQLite.
     */
    fun upsertNotesFromSqlite(sqliteNotes: List<Note>): Int = synchronized(FILE_LOCK) {
        if (sqliteNotes.isEmpty()) return 0

        val existingNotesMap = readNotesFromFile().associateBy { it.id }.toMutableMap()

        var syncedCount = 0
        for (sqliteNote in sqliteNotes) {
            val jsonVersion = Note(
                id = sqliteNote.id,
                title = sqliteNote.title,
                content = sqliteNote.content,
                timestamp = sqliteNote.timestamp,
                storageType = StorageType.JSON
            )
            existingNotesMap[sqliteNote.id] = jsonVersion
            syncedCount++
        }

        val success = writeNotesToFile(existingNotesMap.values.toList())
        return if (success) syncedCount else 0
    }

    /**
     * Reads all notes from the internal JSON file.
     */
    private fun readNotesFromFile(): List<Note> {
        val file = notesFile
        if (!file.exists() || file.length() == 0L) {
            return emptyList()
        }

        val result = mutableListOf<Note>()
        try {
            FileInputStream(file).use { fis ->
                InputStreamReader(fis, Charsets.UTF_8).use { reader ->
                    val jsonString = reader.readText()
                    if (jsonString.isNotBlank()) {
                        val jsonArray = JSONArray(jsonString)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val note = Note(
                                id = obj.getLong(KEY_ID),
                                title = obj.getString(KEY_TITLE),
                                content = obj.getString(KEY_CONTENT),
                                timestamp = obj.optLong(KEY_TIMESTAMP, System.currentTimeMillis()),
                                storageType = StorageType.JSON
                            )
                            result.add(note)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * Writes the given list of notes to the internal JSON file.
     * Returns true if successfully written, false otherwise.
     */
    private fun writeNotesToFile(notes: List<Note>): Boolean {
        val jsonArray = JSONArray()
        for (note in notes) {
            val obj = JSONObject().apply {
                put(KEY_ID, note.id)
                put(KEY_TITLE, note.title)
                put(KEY_CONTENT, note.content)
                put(KEY_TIMESTAMP, note.timestamp)
            }
            jsonArray.put(obj)
        }

        return try {
            FileOutputStream(notesFile, false).use { fos ->
                OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                    writer.write(jsonArray.toString(2))
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
