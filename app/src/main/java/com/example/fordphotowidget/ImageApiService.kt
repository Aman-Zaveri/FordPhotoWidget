package com.example.fordphotowidget

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Since placehold.co doesn't have a JSON API, we'll generate URLs directly
object PlaceholderImageGenerator {
    fun generatePlaceholderUrls(count: Int): List<PlaceholderImage> {
        val images = mutableListOf<PlaceholderImage>()

        for (i in 1..count) {
            val width = (300..800).random()
            val height = (300..800).random()
            val bgColor = String.format("%06x", (Math.random() * 0xFFFFFF).toInt())
            val textColor = String.format("%06x", (Math.random() * 0xFFFFFF).toInt())
            val text = "Ford"
            val encodedText = java.net.URLEncoder.encode(text, "UTF-8")

            // Add .png extension explicitly to ensure correct format
            val url = "https://placehold.co/${width}x${height}/0400ff/FFFFFF.png?text=$encodedText"
            images.add(PlaceholderImage(
                id = i.toString(),
                width = width,
                height = height,
                url = url
            ))
        }
        return images
    }
}

// Simple data class for placeholder images
data class PlaceholderImage(
    val id: String,
    val width: Int,
    val height: Int,
    val url: String
)

// Keep RetrofitClient for potential future API usage
object RetrofitClient {
    private const val BASE_URL = "https://picsum.photos/"

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logger)
        .build()
}