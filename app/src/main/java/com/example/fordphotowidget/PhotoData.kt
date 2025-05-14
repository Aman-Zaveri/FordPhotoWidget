package com.example.fordphotowidget

import android.net.Uri

data class PhotoData(
    val uri: Uri,
    val name: String,
    val size: Long,
    val date: Long
)