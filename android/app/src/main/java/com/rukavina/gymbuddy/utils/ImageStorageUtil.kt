package com.rukavina.gymbuddy.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageUtil {

    private const val TAG = "ImageStorageUtil"
    private const val PROFILE_IMAGES_DIR = "profile_images"

    /**
     * Copies an image from a content URI to app's internal storage.
     * Returns the absolute path to the saved file, or null if failed.
     */
    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        Log.d(TAG, "saveImageToInternalStorage called with uri: $uri")
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for uri: $uri")
                return null
            }

            // Create directory if it doesn't exist
            val directory = File(context.filesDir, PROFILE_IMAGES_DIR)
            if (!directory.exists()) {
                val created = directory.mkdirs()
                Log.d(TAG, "Created directory: $directory, success: $created")
            }

            // Delete old profile images (keep only one)
            directory.listFiles()?.forEach {
                Log.d(TAG, "Deleting old file: ${it.absolutePath}")
                it.delete()
            }

            // Create new file with unique name
            val fileName = "profile_${UUID.randomUUID()}.jpg"
            val file = File(directory, fileName)
            Log.d(TAG, "Saving to file: ${file.absolutePath}")

            // Copy image data
            FileOutputStream(file).use { outputStream ->
                val bytes = inputStream.copyTo(outputStream)
                Log.d(TAG, "Copied $bytes bytes")
            }
            inputStream.close()

            Log.d(TAG, "Successfully saved image to: ${file.absolutePath}, exists: ${file.exists()}, size: ${file.length()}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image", e)
            null
        }
    }

    /**
     * Deletes the profile image if it exists.
     */
    fun deleteProfileImage(context: Context, imagePath: String?) {
        if (imagePath == null) return
        try {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
