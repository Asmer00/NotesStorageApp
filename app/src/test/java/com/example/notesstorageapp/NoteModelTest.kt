package com.example.notesstorageapp

import com.example.notesstorageapp.model.Note
import com.example.notesstorageapp.model.StorageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NoteModelTest {

    @Test
    fun testNoteCreation() {
        val timestamp = System.currentTimeMillis()
        val note = Note(
            id = 1L,
            title = "Test Note",
            content = "This is a test note content.",
            timestamp = timestamp,
            storageType = StorageType.SQLITE
        )

        assertEquals(1L, note.id)
        assertEquals("Test Note", note.title)
        assertEquals("This is a test note content.", note.content)
        assertEquals(timestamp, note.timestamp)
        assertEquals(StorageType.SQLITE, note.storageType)
    }

    @Test
    fun testNoteUpdateCopy() {
        val originalNote = Note(
            id = 100L,
            title = "Original Title",
            content = "Original Content",
            timestamp = 1000L,
            storageType = StorageType.SQLITE
        )

        val updatedNote = originalNote.copy(
            title = "Updated Title",
            content = "Updated Content",
            timestamp = 2000L
        )

        assertEquals(100L, updatedNote.id)
        assertEquals("Updated Title", updatedNote.title)
        assertEquals("Updated Content", updatedNote.content)
        assertEquals(2000L, updatedNote.timestamp)
        assertEquals(StorageType.SQLITE, updatedNote.storageType)
    }

    @Test
    fun testNoteStorageMigration() {
        val sqliteNote = Note(
            id = 42L,
            title = "Migrating Note",
            content = "Moving to JSON storage",
            timestamp = 5000L,
            storageType = StorageType.SQLITE
        )

        val jsonNote = sqliteNote.copy(storageType = StorageType.JSON)

        assertEquals(sqliteNote.id, jsonNote.id)
        assertEquals(sqliteNote.title, jsonNote.title)
        assertEquals(StorageType.JSON, jsonNote.storageType)
    }

    @Test
    fun testMonotonicUniqueIdGenerationLogic() {
        // Simulates the ID generation algorithm in JsonNoteStorage
        val existingNotes = mutableListOf<Note>()
        val baseTime = 1000000L

        // Even if multiple notes are generated at the exact same base timestamp
        for (i in 1..5) {
            val maxExistingId = existingNotes.maxOfOrNull { it.id } ?: 0L
            val newId = if (baseTime > maxExistingId) baseTime else maxExistingId + 1L
            existingNotes.add(
                Note(
                    id = newId,
                    title = "Note $i",
                    content = "Content $i",
                    timestamp = baseTime,
                    storageType = StorageType.JSON
                )
            )
        }

        // Verify all 5 notes have strictly unique IDs
        val ids = existingNotes.map { it.id }
        assertEquals(5, ids.distinct().size)
        // Verify IDs are strictly increasing
        assertEquals(listOf(1000000L, 1000001L, 1000002L, 1000003L, 1000004L), ids)
    }
}
