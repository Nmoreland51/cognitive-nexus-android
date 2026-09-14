package com.nmoreland.cognitivenexus.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.nmoreland.cognitivenexus.data.api.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.io.ByteArrayOutputStream

class NexusRepository(private val settings: BackendSettingsRepository) {
    suspend fun api() = NetworkClient.createApi(settings.connection.first())
    suspend fun saveImage(id: String, output: java.io.OutputStream) = withContext(Dispatchers.IO) {
        api().image(id).use { response ->
            require(response.contentLength() <= 20_000_000) { "Image exceeds the 20 MB download limit." }
            response.byteStream().use { input ->
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= 20_000_000) { "Image exceeds the 20 MB download limit." }
                    output.write(buffer, 0, count)
                }
            }
        }
    }
    suspend fun image(id: String): Bitmap = withContext(Dispatchers.IO) {
        api().image(id).use { response ->
            require(response.contentLength() <= 20_000_000) { "Image is too large to preview." }
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            response.byteStream().use { stream ->
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 20_000_000) { "Image is too large to preview." }
                    output.write(buffer, 0, count)
                }
            }
            val bytes = output.toByteArray()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = 1
                while (bounds.outWidth / inSampleSize > 1600 || bounds.outHeight / inSampleSize > 1600) inSampleSize *= 2
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: error("Image could not be decoded.")
        }
    }
}

fun readableError(error: Throwable): String = when (error) {
    is HttpException -> when (error.code()) {
        401, 403 -> "Access denied. Check the backend access token in Settings."
        404 -> "This route/task does not exist. Use the new Mobile API v2 adapter, not the old demo backend."
        413 -> "File is too large. Choose UTF-8 text smaller than 1 MB."
        422 -> "The backend rejected these options. Check text, file type, and number ranges."
        429 -> "The backend task queue is full. Wait, then try again."
        else -> "Backend returned HTTP ${error.code()}. Check server diagnostics."
    }
    is IOException -> "Cannot reach the backend. Check its URL, Wi-Fi/VPN, server process, and firewall."
    else -> error.message ?: "The operation could not be completed."
}
