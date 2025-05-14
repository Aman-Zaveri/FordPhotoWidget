package com.example.fordphotowidget

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity

class GalleryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PhotoAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var darkOverlay: View
    private lateinit var downloadButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private val imageList = mutableListOf<PhotoData>()

    // Content observer to detect media changes instead of broadcast receiver
    private val contentObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refreshGallery()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_gallery, container, false)

        // Initialize the toolbar
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.headerToolbar)

        // Rest of your existing initialization code
        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        downloadButton = view.findViewById(R.id.downloadButton)
        darkOverlay = view.findViewById(R.id.darkOverlay)



        // Add hover and click effects
        downloadButton.apply {
            // Increase elevation when pressed for more pronounced effect
            this.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        // When button is pressed, animate to a larger elevation
                        this.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start()
                        v.elevation = 12f
                        false
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        // When touch is released, return to original size
                        this.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        v.elevation = 6f
                        false
                    }
                    else -> false
                }
            }
        }

        // Update your XML layout for better visual feedback
        downloadButton.apply {
            compatElevation = 6f
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_blue_dark)
            rippleColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize adapter with empty list first
        adapter = PhotoAdapter(imageList)
        recyclerView.adapter = adapter

        // Set up download button
        downloadButton.setOnClickListener {
            downloadImagesFromRemoteServer()
        }

        // Load images
        loadImagesFromStorage()

        return view
    }

    private fun downloadImagesFromRemoteServer() {
        progressBar.visibility = View.VISIBLE
        darkOverlay.visibility = View.VISIBLE
        downloadButton.isEnabled = false

        // Add log to track execution
        Log.d("GalleryFragment", "Starting image download")

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Increase number of images and explicitly request JPG format
                val placeholderImages = PlaceholderImageGenerator.generatePlaceholderUrls(1)

                Log.d("GalleryFragment", "Generated URLs: ${placeholderImages.map { it.url }}")

                // Download each image
                for (image in placeholderImages) {
                    try {
                        val imageUrl = URL(image.url)
                        Log.d("GalleryFragment", "Downloading from: ${image.url}")

                        val connection = imageUrl.openConnection()
                        connection.connect()

                        val inputStream = connection.getInputStream()

                        // Create a file in Pictures directory
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val filename = "Placeholder_${image.width}x${image.height}_${timestamp}.png"

                        // Public Pictures directory
                        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        val appDir = File(picturesDir, "FordPhotoWidget")
                        if (!appDir.exists()) {
                            appDir.mkdirs()
                        }

                        val outputFile = File(appDir, filename)
                        Log.d("GalleryFragment", "Saving to: ${outputFile.absolutePath}")

                        val outputStream = FileOutputStream(outputFile)

                        inputStream.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }

                        // Use MediaScanner with correct MIME type (PNG)
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(outputFile.toString()),
                            arrayOf("image/png"),
                            null
                        )

                        Log.d("GalleryFragment", "MediaScanner completed for: ${outputFile.name}")

                        // Force update
                        withContext(Dispatchers.Main) {
                            refreshGallery()
                        }
                    } catch (e: Exception) {
                        Log.e("GalleryFragment", "Error downloading image: ${e.message}", e)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Placeholder images downloaded successfully", Toast.LENGTH_SHORT).show()
                    refreshGallery()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("GalleryFragment", "Error downloading images", e)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    darkOverlay.visibility = View.GONE
                    downloadButton.isEnabled = true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            // Register content observer instead of broadcast receiver
            requireContext().contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                contentObserver
            )

            refreshGallery()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            // Unregister content observer
            requireContext().contentResolver.unregisterContentObserver(contentObserver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun refreshGallery() {
        imageList.clear()
        loadImagesFromStorage()
        adapter.notifyDataSetChanged()
    }

    private fun loadImagesFromStorage() {
        try {
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
        } catch (e: Exception) {
            Log.e("GalleryFragment", "Error loading images", e)
        }
    }
}