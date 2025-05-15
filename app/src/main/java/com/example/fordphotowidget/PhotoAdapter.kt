package com.example.fordphotowidget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.Locale

class PhotoAdapter(private val photos: List<PhotoData>) :
    RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val textName: TextView = view.findViewById(R.id.textName)
        val textFileType: TextView = view.findViewById(R.id.textFileType)
        val textDimensions: TextView = view.findViewById(R.id.textDimensions)
        val textSize: TextView = view.findViewById(R.id.textSize)
        val textDate: TextView = view.findViewById(R.id.textDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]
        val context = holder.itemView.context

        // Load image with Glide
        Glide.with(context)
            .load(photo.uri)
            .centerCrop()
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.imageView)

        // Set basic metadata
        holder.textName.text = photo.name

        // Extract and display file extension
        val fileExtension = photo.name.substringAfterLast('.', "unknown")
        holder.textFileType.text = "Type: ${fileExtension.uppercase()}"

        // Format size nicely
        val sizeInKB = photo.size / 1024
        val sizeText = if (sizeInKB > 1024) {
            String.format("Size: %.1f MB", sizeInKB / 1024f)
        } else {
            "Size: $sizeInKB KB"
        }
        holder.textSize.text = sizeText

        // Format date with both date and time
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        holder.textDate.text = "Added: ${sdf.format(photo.date * 1000)}"

        // Get image dimensions using Glide
        Glide.with(context)
            .asBitmap()
            .load(photo.uri)
            .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                override fun onResourceReady(
                    resource: android.graphics.Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?
                ) {
                    holder.textDimensions.text = "Dimensions: ${resource.width} × ${resource.height}"
                }

                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                    // Do nothing
                }
            })
    }

    override fun getItemCount(): Int = photos.size
}