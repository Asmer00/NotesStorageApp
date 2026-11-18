package com.example.notesstorageapp.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notesstorageapp.R
import com.example.notesstorageapp.databinding.ActivityMainBinding
import com.example.notesstorageapp.model.Note
import com.example.notesstorageapp.model.StorageType
import com.example.notesstorageapp.storage.NoteRepository
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

/**
 * Main Activity displaying the list of notes, tab filters (All / SQLite / JSON),
 * sync trigger, and note management actions.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: NoteRepository
    private lateinit var noteAdapter: NoteAdapter

    // Current filter for notes: null = All, SQLITE = SQLite only, JSON = JSON only
    private var currentStorageFilter: StorageType? = null

    // Activity Result Launcher for launching NoteEditorActivity
    private val editorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadNotes()
            val actionType = result.data?.getStringExtra(NoteEditorActivity.EXTRA_ACTION_TYPE)
            val messageResId = when (actionType) {
                NoteEditorActivity.ACTION_TYPE_SAVED -> R.string.msg_note_saved
                NoteEditorActivity.ACTION_TYPE_UPDATED -> R.string.msg_note_updated
                NoteEditorActivity.ACTION_TYPE_DELETED -> R.string.msg_note_deleted
                else -> null
            }
            messageResId?.let { resId ->
                Snackbar.make(binding.root, getString(resId), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = NoteRepository.getInstance(this)

        setupToolbar()
        setupRecyclerView()
        setupTabLayout()
        setupFab()
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(
            onNoteClicked = { note ->
                val intent = NoteEditorActivity.newIntent(this, note)
                editorLauncher.launch(intent)
            },
            onDeleteClicked = { note ->
                confirmDeleteNote(note)
            }
        )

        binding.rvNotes.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = noteAdapter
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentStorageFilter = when (tab?.position) {
                    1 -> StorageType.SQLITE
                    2 -> StorageType.JSON
                    else -> null
                }
                loadNotes()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupFab() {
        binding.fabAddNote.setOnClickListener {
            val intent = NoteEditorActivity.newIntent(this)
            editorLauncher.launch(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                performSync()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadNotes() {
        val notes = repository.getNotes(currentStorageFilter)
        noteAdapter.submitList(notes)

        if (notes.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvNotes.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvNotes.visibility = View.VISIBLE
        }
    }

    private fun performSync() {
        try {
            val syncedCount = repository.syncSqliteToJson()
            loadNotes()

            val message = if (syncedCount > 0) {
                getString(R.string.msg_sync_success, syncedCount)
            } else {
                getString(R.string.msg_sync_empty)
            }

            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        } catch (e: Exception) {
            val fallbackError = getString(R.string.error_unknown)
            Snackbar.make(
                binding.root,
                getString(R.string.msg_error, e.localizedMessage ?: fallbackError),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun confirmDeleteNote(note: Note) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_delete_title))
            .setMessage(getString(R.string.dialog_delete_message, note.title))
            .setPositiveButton(getString(R.string.dialog_btn_delete)) { _, _ ->
                val deleted = repository.deleteNote(note)
                if (deleted) {
                    loadNotes()
                    Snackbar.make(
                        binding.root,
                        getString(R.string.msg_note_deleted),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.dialog_btn_cancel), null)
            .show()
    }
}
