package com.example.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object ImageStorageHelper {
    fun copyImageToAppStorage(context: Context, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "product_images")
            if (!dir.exists()) dir.mkdirs()
            val target = File(dir, "prod_${UUID.randomUUID().toString().take(10)}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            target.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
