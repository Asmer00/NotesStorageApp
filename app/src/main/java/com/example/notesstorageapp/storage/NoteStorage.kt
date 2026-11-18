package com.example.notesstorageapp.storage

import com.example.notesstorageapp.model.Note

/**
 * Common storage interface abstracting CRUD operations for notes.
 */
interface NoteStorage {
    /**
     * Retrieves all notes stored in this engine.
     */
    fun getAllNotes(): List<Note>

    /**
     * Retrieves a single note by its ID, or null if not found.
     */
    fun getNoteById(id: Long): Note?

    /**
     * Inserts a new note with the given title and content.
     */
    fun insertNote(title: String, content: String): Note

    /**
     * Updates an existing note. Returns true if successful.
     */
    fun updateNote(note: Note): Boolean

    /**
     * Deletes a note by its ID. Returns true if deleted.
     */
    fun deleteNote(id: Long): Boolean
}
