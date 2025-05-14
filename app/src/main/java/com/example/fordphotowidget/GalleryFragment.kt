package com.example.fordphotowidget

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GalleryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PhotoAdapter
    private val imageList = mutableListOf<PhotoData>()

    // Broadcast receiver to detect media changes
    private val imageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Refresh the gallery when new images are added
            refreshGallery()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_gallery, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize adapter with empty list first
        adapter = PhotoAdapter(imageList)
        recyclerView.adapter = adapter

        // Load images
        loadImagesFromStorage()

        return view
    }

    override fun onResume() {
        super.onResume()
        try {
            // Modify your broadcast receiver registration
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED)
            }
            // Remove the data scheme that could be causing issues
            // addDataScheme("file") - This line is problematic

            ContextCompat.registerReceiver(
                requireContext(),
                imageReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

            // Always refresh on resume to catch any changes
            refreshGallery()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister when the fragment is not visible
        try {
            requireContext().unregisterReceiver(imageReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    private fun refreshGallery() {
        // Clear current list and load fresh data
        imageList.clear()
        loadImagesFromStorage()
        adapter.notifyDataSetChanged()
    }

    private fun loadImagesFromStorage() {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = requireContext().contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val size = it.getLong(sizeColumn)
                val date = it.getLong(dateColumn)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )

                imageList.add(PhotoData(contentUri, name, size, date))
            }
        }
    }
}