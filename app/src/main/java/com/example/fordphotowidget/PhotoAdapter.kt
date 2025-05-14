package com.example.fordphotowidget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PhotoAdapter(private val photos: List<PhotoData>) :
    RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val textName: TextView = view.findViewById(R.id.textName)
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

        holder.imageView.setImageURI(photo.uri)
        holder.textName.text = photo.name
        holder.textSize.text = "Size: ${photo.size / 1024} KB"
        holder.textDate.text = "Date: ${java.text.SimpleDateFormat("dd/MM/yyyy").format(photo.date * 1000)}"
    }

    override fun getItemCount(): Int = photos.size
}