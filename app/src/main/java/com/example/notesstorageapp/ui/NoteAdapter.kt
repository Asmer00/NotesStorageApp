package com.example.notesstorageapp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.notesstorageapp.R
import com.example.notesstorageapp.databinding.ItemNoteBinding
import com.example.notesstorageapp.model.Note
import com.example.notesstorageapp.model.StorageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for displaying notes with storage indicators and action triggers.
 */
class NoteAdapter(
    private val onNoteClicked: (Note) -> Unit,
    private val onDeleteClicked: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private val notes = mutableListOf<Note>()
    private val dateFormat = SimpleDateFormat("MMM d, yyyy, HH:mm", Locale.getDefault())

    fun submitList(newNotes: List<Note>) {
        val diffCallback = NoteDiffCallback(notes, newNotes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        notes.clear()
        notes.addAll(newNotes)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            val context = itemView.context

            binding.tvNoteTitle.text = note.title
            binding.tvNoteContent.text = note.content
            binding.tvNoteTimestamp.text = dateFormat.format(Date(note.timestamp))

            // Storage badge styling
            when (note.storageType) {
                StorageType.SQLITE -> {
                    binding.tvStorageBadge.text = context.getString(R.string.badge_sqlite)
                    binding.tvStorageBadge.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.badge_sqlite_bg)
                    binding.tvStorageBadge.setTextColor(
                        ContextCompat.getColor(context, R.color.badge_sqlite_text)
                    )
                }
                StorageType.JSON -> {
                    binding.tvStorageBadge.text = context.getString(R.string.badge_json)
                    binding.tvStorageBadge.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.badge_json_bg)
                    binding.tvStorageBadge.setTextColor(
                        ContextCompat.getColor(context, R.color.badge_json_text)
                    )
                }
            }

            // Click listener to view/edit note
            binding.cardNote.setOnClickListener {
                onNoteClicked(note)
            }

            // Click listener to delete note
            binding.btnDeleteNote.setOnClickListener {
                onDeleteClicked(note)
            }
        }
    }

    private class NoteDiffCallback(
        private val oldList: List<Note>,
        private val newList: List<Note>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.id == newItem.id && oldItem.storageType == newItem.storageType
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
