package com.shnwaz.lyrasave.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

object MediaUtils {

    fun getVideoDuration(context: Context, uri: Uri): String {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            retriever.release()
            formatMs(ms)
        } catch (e: Exception) {
            "0:00"
        }
    }

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%d:%02d".format(min, sec)
    }

    fun openInputStream(context: Context, uri: Uri): java.io.InputStream? {
        return try {
            if (uri.scheme == "file" && uri.path != null) {
                java.io.FileInputStream(java.io.File(uri.path!!))
            } else {
                context.contentResolver.openInputStream(uri)
                    ?: (if (uri.path != null) java.io.FileInputStream(java.io.File(uri.path!!)) else null)
            }
        } catch (e: Exception) {
            try {
                if (uri.path != null) java.io.FileInputStream(java.io.File(uri.path!!)) else null
            } catch (ex: Exception) {
                null
            }
        }
    }

    fun createShareUri(context: Context, uri: Uri, fileName: String): Uri? {
        return try {
            val cache = java.io.File(context.cacheDir, "shared").apply { mkdirs() }
            val tmp = java.io.File(cache, fileName)
            val inStream = openInputStream(context, uri) ?: return null
            inStream.use { inp ->
                tmp.outputStream().use { out -> inp.copyTo(out) }
            }
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tmp
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}