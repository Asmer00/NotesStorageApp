package com.example.notesstorageapp.storage.sqlite

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.notesstorageapp.model.Note
import com.example.notesstorageapp.model.StorageType
import com.example.notesstorageapp.storage.NoteStorage

/**
 * SQLite implementation of NoteStorage using Android's native SQLiteOpenHelper.
 */
class SqliteNoteStorage(context: Context) : NoteStorage {

    private val dbHelper = NotesDbHelper(context.applicationContext)

    override fun getAllNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        val db = dbHelper.readableDatabase

        val projection = arrayOf(
            NotesDbHelper.COLUMN_ID,
            NotesDbHelper.COLUMN_TITLE,
            NotesDbHelper.COLUMN_CONTENT,
            NotesDbHelper.COLUMN_TIMESTAMP
        )

        val sortOrder = "${NotesDbHelper.COLUMN_TIMESTAMP} DESC"

        db.query(
            NotesDbHelper.TABLE_NOTES,
            projection,
            null,
            null,
            null,
            null,
            sortOrder
        ).use { cursor ->
            while (cursor.moveToNext()) {
                notes.add(cursorToNote(cursor))
            }
        }

        return notes
    }

    override fun getNoteById(id: Long): Note? {
        val db = dbHelper.readableDatabase
        val selection = "${NotesDbHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        val projection = arrayOf(
            NotesDbHelper.COLUMN_ID,
            NotesDbHelper.COLUMN_TITLE,
            NotesDbHelper.COLUMN_CONTENT,
            NotesDbHelper.COLUMN_TIMESTAMP
        )

        db.query(
            NotesDbHelper.TABLE_NOTES,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursorToNote(cursor)
            }
        }

        return null
    }

    override fun insertNote(title: String, content: String): Note {
        val db = dbHelper.writableDatabase
        val timestamp = System.currentTimeMillis()

        val values = ContentValues().apply {
            put(NotesDbHelper.COLUMN_TITLE, title)
            put(NotesDbHelper.COLUMN_CONTENT, content)
            put(NotesDbHelper.COLUMN_TIMESTAMP, timestamp)
        }

        val newRowId = db.insert(NotesDbHelper.TABLE_NOTES, null, values)
        return Note(
            id = newRowId,
            title = title,
            content = content,
            timestamp = timestamp,
            storageType = StorageType.SQLITE
        )
    }

    override fun updateNote(note: Note): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(NotesDbHelper.COLUMN_TITLE, note.title)
            put(NotesDbHelper.COLUMN_CONTENT, note.content)
            put(NotesDbHelper.COLUMN_TIMESTAMP, System.currentTimeMillis())
        }

        val selection = "${NotesDbHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(note.id.toString())

        val count = db.update(NotesDbHelper.TABLE_NOTES, values, selection, selectionArgs)
        return count > 0
    }

    override fun deleteNote(id: Long): Boolean {
        val db = dbHelper.writableDatabase
        val selection = "${NotesDbHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        val count = db.delete(NotesDbHelper.TABLE_NOTES, selection, selectionArgs)
        return count > 0
    }

    private fun cursorToNote(cursor: Cursor): Note {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_ID))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_TITLE))
        val content = cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_CONTENT))
        val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_TIMESTAMP))

        return Note(
            id = id,
            title = title,
            content = content,
            timestamp = timestamp,
            storageType = StorageType.SQLITE
        )
    }
}
