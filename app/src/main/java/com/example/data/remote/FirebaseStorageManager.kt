package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object FirebaseStorageManager {

    private val storage = FirebaseStorage.getInstance()
    private val itemsRef = storage.reference.child("items")

    suspend fun uploadItemImage(
        context: Context,
        itemId: String,
        imageUri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val compressedBytes = context.contentResolver.openInputStream(imageUri)?.use { stream ->
                val originalBitmap = BitmapFactory.decodeStream(stream) ?: return@use null
                val maxDim = 800
                val ratio = minOf(1f, maxDim.toFloat() / maxOf(originalBitmap.width, originalBitmap.height))
                val scaled = Bitmap.createScaledBitmap(
                    originalBitmap,
                    (originalBitmap.width * ratio).toInt(),
                    (originalBitmap.height * ratio).toInt(),
                    true
                )
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
                out.toByteArray()
            } ?: return@withContext Result.failure(Exception("Unable to decode selected image"))

            val fileRef = itemsRef.child("$itemId.jpg")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            fileRef.putBytes(compressedBytes, metadata).await()
            val downloadUrl = fileRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteItemImage(itemId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            itemsRef.child("$itemId.jpg").delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
