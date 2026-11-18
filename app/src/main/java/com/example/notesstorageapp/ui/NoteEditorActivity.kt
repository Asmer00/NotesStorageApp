package com.example.notesstorageapp.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.notesstorageapp.R
import com.example.notesstorageapp.databinding.ActivityNoteEditorBinding
import com.example.notesstorageapp.model.Note
import com.example.notesstorageapp.model.StorageType
import com.example.notesstorageapp.storage.NoteRepository
import com.google.android.material.snackbar.Snackbar

/**
 * Activity for creating a new note or editing an existing note.
 * Handles input validation, storage engine selection, and deletion.
 */
class NoteEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_ACTION_TYPE = "extra_action_type"
        const val ACTION_TYPE_SAVED = "action_saved"
        const val ACTION_TYPE_UPDATED = "action_updated"
        const val ACTION_TYPE_DELETED = "action_deleted"

        fun newIntent(context: Context, note: Note? = null): Intent {
            return Intent(context, NoteEditorActivity::class.java).apply {
                if (note != null) {
                    putExtra(EXTRA_NOTE, note)
                }
            }
        }
    }

    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var repository: NoteRepository
    private var existingNote: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = NoteRepository.getInstance(this)

        // Retrieve existing note if in edit mode
        existingNote = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_NOTE, Note::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_NOTE) as? Note
        }

        setupToolbar()
        setupViews()
        setupValidationWatchers()
        setupBackNavigation()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarEditor)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        title = if (existingNote != null) {
            getString(R.string.title_edit_note)
        } else {
            getString(R.string.title_create_note)
        }
    }

    private fun setupViews() {
        val note = existingNote
        if (note != null) {
            binding.etNoteTitle.setText(note.title)
            binding.etNoteContent.setText(note.content)

            when (note.storageType) {
                StorageType.SQLITE -> binding.rbStorageSqlite.isChecked = true
                StorageType.JSON -> binding.rbStorageJson.isChecked = true
            }
        } else {
            binding.rbStorageSqlite.isChecked = true
        }

        binding.btnSaveNote.setOnClickListener {
            validateAndSaveNote()
        }
    }

    private fun setupValidationWatchers() {
        binding.etNoteTitle.doAfterTextChanged {
            if (!it.isNullOrBlank()) {
                binding.tilNoteTitle.error = null
            }
        }

        binding.etNoteContent.doAfterTextChanged {
            if (!it.isNullOrBlank()) {
                binding.tilNoteContent.error = null
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                checkUnsavedChangesAndExit()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_note_editor, menu)
        // Show delete option only if editing an existing note
        menu.findItem(R.id.action_delete)?.isVisible = (existingNote != null)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                checkUnsavedChangesAndExit()
                true
            }
            R.id.action_save -> {
                validateAndSaveNote()
                true
            }
            R.id.action_delete -> {
                confirmDeleteNote()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun validateAndSaveNote() {
        val title = binding.etNoteTitle.text?.toString()?.trim().orEmpty()
        val content = binding.etNoteContent.text?.toString()?.trim().orEmpty()

        var hasError = false

        if (title.isEmpty()) {
            binding.tilNoteTitle.error = getString(R.string.error_empty_title)
            hasError = true
        } else {
            binding.tilNoteTitle.error = null
        }

        if (content.isEmpty()) {
            binding.tilNoteContent.error = getString(R.string.error_empty_content)
            hasError = true
        } else {
            binding.tilNoteContent.error = null
        }

        if (hasError) {
            Snackbar.make(
                binding.root,
                getString(R.string.error_validation_failed),
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val targetStorage = if (binding.rbStorageSqlite.isChecked) {
            StorageType.SQLITE
        } else {
            StorageType.JSON
        }

        val note = existingNote
        val actionType = if (note == null) {
            // Create new note
            repository.saveNewNote(title, content, targetStorage)
            ACTION_TYPE_SAVED
        } else {
            // If storage type changed, migrate the note safely (save to target first, then delete from source)
            if (note.storageType != targetStorage) {
                repository.saveNewNote(title, content, targetStorage)
                repository.deleteNote(note)
            } else {
                // Update note in existing storage
                val updatedNote = note.copy(
                    title = title,
                    content = content
                )
                repository.updateNote(updatedNote)
            }
            ACTION_TYPE_UPDATED
        }

        val resultIntent = Intent().apply {
            putExtra(EXTRA_ACTION_TYPE, actionType)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun confirmDeleteNote() {
        val note = existingNote ?: return

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_delete_title))
            .setMessage(getString(R.string.dialog_delete_message, note.title))
            .setPositiveButton(getString(R.string.dialog_btn_delete)) { _, _ ->
                repository.deleteNote(note)
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_DELETED)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            .setNegativeButton(getString(R.string.dialog_btn_cancel), null)
            .show()
    }

    private fun checkUnsavedChangesAndExit() {
        val currentTitle = binding.etNoteTitle.text?.toString()?.trim().orEmpty()
        val currentContent = binding.etNoteContent.text?.toString()?.trim().orEmpty()
        val currentStorage = if (binding.rbStorageSqlite.isChecked) StorageType.SQLITE else StorageType.JSON

        val originalTitle = existingNote?.title.orEmpty()
        val originalContent = existingNote?.content.orEmpty()
        val originalStorage = existingNote?.storageType ?: StorageType.SQLITE

        val hasChanges = if (existingNote != null) {
            currentTitle != originalTitle || currentContent != originalContent || currentStorage != originalStorage
        } else {
            currentTitle.isNotEmpty() || currentContent.isNotEmpty()
        }

        if (hasChanges) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_discard_title))
                .setMessage(getString(R.string.dialog_discard_message))
                .setPositiveButton(getString(R.string.dialog_btn_discard)) { _, _ ->
                    finish()
                }
                .setNegativeButton(getString(R.string.dialog_btn_keep_editing), null)
                .show()
        } else {
            finish()
        }
    }
}
