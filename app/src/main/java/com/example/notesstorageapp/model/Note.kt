package com.example.notesstorageapp.model

import java.io.Serializable

/**
 * Data model representing a note in the application.
 *
 * @property id Unique identifier for the note.
 * @property title Title of the note.
 * @property content Body/text content of the note.
 * @property timestamp Epoch millisecond timestamp of creation or last update.
 * @property storageType The storage engine where this note is persisted.
 */
data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val storageType: StorageType
) : Serializable
