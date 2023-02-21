package com.example.oa_wallet_android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class DocumentRVAdapter (private val documents: List<File>) : RecyclerView.Adapter<DocumentRVAdapter.ViewHolder>() {
    var onItemTap: ((File) -> Unit)? = null
    var onOptionsTap: ((File) -> Unit)? = null

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.listitem_document, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val document = documents[position]
        holder.textView.text = document.name
        holder.itemView.setOnClickListener {
            onItemTap?.invoke(document)
        }
        holder.optionsImageView.setOnClickListener {
            onOptionsTap?.invoke(document)
        }
    }

    override fun getItemCount(): Int {
        return documents.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val textView: TextView = itemView.findViewById(R.id.filenameTextView)
        val optionsImageView: ImageView = itemView.findViewById(R.id.optionsImageView)
    }
}