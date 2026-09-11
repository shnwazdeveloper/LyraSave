package com.shnwaz.lyrasave.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.shnwaz.lyrasave.model.StatusItem
import com.shnwaz.lyrasave.utils.MediaUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class StatusRepository(private val context: Context) {

    companion object {
        private const val PREF_NAME = "lyra_save_prefs"
        private const val KEY_URI   = "whatsapp_tree_uri"
        private const val KEY_SOURCE = "whatsapp_source_type"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val SAVE_PICTURES_DIR = "LyraSave"
        const val SAVE_MOVIES_DIR   = "LyraSave"

        const val SOURCE_WHATSAPP = 0
        const val SOURCE_WHATSAPP_BUSINESS = 1
        const val SOURCE_DUAL = 2
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // --- Onboarding Persistence -------------------------------------------------

    fun isOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    // --- URI Persistence --------------------------------------------------------

    fun getPersistedUri(): Uri? {
        val s = prefs.getString(KEY_URI, null) ?: return null
        return Uri.parse(s)
    }

    fun persistUri(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        prefs.edit().putString(KEY_URI, uri.toString()).apply()
    }

    fun clearPersistedUri() {
        prefs.edit().remove(KEY_URI).apply()
    }

    fun getSourceType(): Int = prefs.getInt(KEY_SOURCE, SOURCE_WHATSAPP)
    fun setSourceType(type: Int) = prefs.edit().putInt(KEY_SOURCE, type).apply()

    // --- Load WhatsApp Statuses (SAF + Direct Storage Fallback) -----------------

    fun loadStatuses(treeUri: Uri? = null): List<StatusItem> {
        val savedNames = getSavedFileNames()
        val resultMap = mutableMapOf<String, StatusItem>()

        // 1. Scan direct filesystem locations (fast, highly reliable across Android versions & dual apps)
        scanDirectStorage(resultMap, savedNames)

        // 2. Scan SAF tree if uri is provided or persisted
        val uriToScan = treeUri ?: getPersistedUri()
        if (uriToScan != null) {
            scanSafTree(uriToScan, resultMap, savedNames)
        }

        return resultMap.values.sortedByDescending { it.dateModified }
    }

    private fun scanDirectStorage(resultMap: MutableMap<String, StatusItem>, savedNames: Set<String>) {
        val baseRoots = listOf(
            Environment.getExternalStorageDirectory(),
            File("/storage/emulated/0"),
            File("/storage/emulated/999"), // MIUI / HyperOS Dual Apps
            File("/sdcard")
        )

        val relativeDirs = listOf(
            "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
            "WhatsApp/Media/.Statuses",
            "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
            "WhatsApp Business/Media/.Statuses",
            "Android/media/com.gbwhatsapp/GBWhatsApp/Media/.Statuses",
            "GBWhatsApp/Media/.Statuses",
            "DualApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
            "DualApp/WhatsApp/Media/.Statuses",
            "Android/media/com.whatsapp/WhatsApp/Media/Statuses",
            "WhatsApp/Media/Statuses"
        )

        for (base in baseRoots) {
            if (!base.exists()) continue
            for (rel in relativeDirs) {
                val folder = File(base, rel)
                if (folder.exists() && folder.canRead()) {
                    val files = folder.listFiles() ?: continue
                    for (file in files) {
                        if (!file.isFile || file.name == ".nomedia" || file.length() <= 0L) continue
                        val name = file.name
                        if (!resultMap.containsKey(name) && isValidMediaFile(name)) {
                            val isVideo = isVideoFile(name)
                            val uri = Uri.fromFile(file)
                            val duration = if (isVideo) MediaUtils.getVideoDuration(context, uri) else ""
                            resultMap[name] = StatusItem(
                                uri = uri,
                                name = name,
                                isVideo = isVideo,
                                size = file.length(),
                                dateModified = file.lastModified(),
                                isSaved = savedNames.contains(name),
                                duration = duration
                            )
                        }
                    }
                }
            }
        }
    }

    private fun scanSafTree(treeUri: Uri, resultMap: MutableMap<String, StatusItem>, savedNames: Set<String>) {
        try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return
            if (!root.canRead()) return

            // Search for status folder or status files inside root
            val statusFolders = mutableListOf<DocumentFile>()
            findStatusFolders(root, statusFolders, currentDepth = 0, maxDepth = 3)

            // If no designated .Statuses folder found, scan root itself
            if (statusFolders.isEmpty()) {
                statusFolders.add(root)
            }

            for (folder in statusFolders) {
                for (file in folder.listFiles()) {
                    if (!file.isFile) continue
                    val name = file.name ?: continue
                    if (!resultMap.containsKey(name) && isValidMediaFile(name)) {
                        val isVideo = isVideoFile(name)
                        val duration = if (isVideo) MediaUtils.getVideoDuration(context, file.uri) else ""
                        resultMap[name] = StatusItem(
                            uri = file.uri,
                            name = name,
                            isVideo = isVideo,
                            size = file.length(),
                            dateModified = file.lastModified(),
                            isSaved = savedNames.contains(name),
                            duration = duration
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun findStatusFolders(dir: DocumentFile, result: MutableList<DocumentFile>, currentDepth: Int, maxDepth: Int) {
        val name = dir.name ?: ""
        if (name.equals(".Statuses", ignoreCase = true) || name.equals("Statuses", ignoreCase = true)) {
            result.add(dir)
            return
        }

        // If folder has media files directly, add it
        val files = dir.listFiles()
        val hasMedia = files.any { it.isFile && isValidMediaFile(it.name ?: "") }
        if (hasMedia) {
            result.add(dir)
        }

        if (currentDepth < maxDepth) {
            for (child in files) {
                if (child.isDirectory) {
                    findStatusFolders(child, result, currentDepth + 1, maxDepth)
                }
            }
        }
    }

    private fun isValidMediaFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
               lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".mp4") ||
               lower.endsWith(".3gp") || lower.endsWith(".mkv") || lower.endsWith(".avi") || lower.endsWith(".mov")
    }

    private fun isVideoFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".3gp") || lower.endsWith(".mkv") ||
               lower.endsWith(".avi") || lower.endsWith(".mov")
    }

    // --- Save Status to Device ---------------------------------------------------

    fun saveStatus(sourceUri: Uri, fileName: String, isVideo: Boolean): Boolean {
        // Try MediaStore first (modern, Scoped Storage compliant)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val ok = saveViaMediaStore(sourceUri, fileName, isVideo)
            if (ok) return true
        }

        // Fallback to direct File system (reliable across legacy & custom ROMs)
        return saveViaFileSystem(sourceUri, fileName, isVideo)
    }

    private fun saveViaMediaStore(sourceUri: Uri, fileName: String, isVideo: Boolean): Boolean {
        return try {
            val collection = if (isVideo)
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val mimeType = resolveMimeType(fileName, isVideo)
            val relativePath = if (isVideo) "Movies/$SAVE_MOVIES_DIR" else "Pictures/$SAVE_PICTURES_DIR"

            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val destUri = context.contentResolver.insert(collection, cv) ?: return false

            val inStream = MediaUtils.openInputStream(context, sourceUri) ?: return false

            context.contentResolver.openOutputStream(destUri)?.use { out ->
                inStream.use { inp -> inp.copyTo(out) }
            } ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cv.clear()
                cv.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(destUri, cv, null, null)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun saveViaFileSystem(sourceUri: Uri, fileName: String, isVideo: Boolean): Boolean {
        return try {
            val parent = if (isVideo)
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), SAVE_MOVIES_DIR)
            else
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), SAVE_PICTURES_DIR)

            if (!parent.exists() && !parent.mkdirs()) return false
            val dest = File(parent, fileName)

            val inStream = MediaUtils.openInputStream(context, sourceUri) ?: return false

            inStream.use { inp ->
                FileOutputStream(dest).use { out -> inp.copyTo(out) }
            }

            MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun resolveMimeType(fileName: String, isVideo: Boolean): String {
        if (isVideo) {
            return when {
                fileName.endsWith(".3gp",  true) -> "video/3gpp"
                fileName.endsWith(".mkv",  true) -> "video/x-matroska"
                else -> "video/mp4"
            }
        }
        return when {
            fileName.endsWith(".png",  true) -> "image/png"
            fileName.endsWith(".webp", true) -> "image/webp"
            fileName.endsWith(".gif",  true) -> "image/gif"
            else -> "image/jpeg"
        }
    }

    // --- Saved Statuses ----------------------------------------------------------

    fun getSavedStatuses(): List<StatusItem> {
        val map = mutableMapOf<String, StatusItem>()

        // 1. Query MediaStore
        getSavedViaMediaStore(map)

        // 2. Query Direct Filesystem folder
        getSavedViaFileSystem(map)

        return map.values.sortedByDescending { it.dateModified }
    }

    private fun getSavedViaMediaStore(map: MutableMap<String, StatusItem>) {
        try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED
            )
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"

            // Images
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, selection,
                arrayOf("%$SAVE_PICTURES_DIR%"),
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val id   = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    val name = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                    val size = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                    val date = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))
                    val uri  = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    if (!map.containsKey(name)) {
                        map[name] = StatusItem(uri, name, false, size, date, isSaved = true)
                    }
                }
            }

            // Videos
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, selection,
                arrayOf("%$SAVE_MOVIES_DIR%"),
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val id   = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    val name = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                    val size = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                    val date = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))
                    val uri  = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val dur  = MediaUtils.getVideoDuration(context, uri)
                    if (!map.containsKey(name)) {
                        map[name] = StatusItem(uri, name, true, size, date, isSaved = true, duration = dur)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSavedViaFileSystem(map: MutableMap<String, StatusItem>) {
        val picDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), SAVE_PICTURES_DIR)
        val vidDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), SAVE_MOVIES_DIR)

        picDir.listFiles()?.forEach { f ->
            if (f.isFile && !map.containsKey(f.name)) {
                map[f.name] = StatusItem(Uri.fromFile(f), f.name, false, f.length(), f.lastModified(), isSaved = true)
            }
        }
        vidDir.listFiles()?.forEach { f ->
            if (f.isFile && !map.containsKey(f.name)) {
                val dur = MediaUtils.getVideoDuration(context, Uri.fromFile(f))
                map[f.name] = StatusItem(Uri.fromFile(f), f.name, true, f.length(), f.lastModified(), isSaved = true, duration = dur)
            }
        }
    }

    fun isStatusAlreadySaved(fileName: String): Boolean {
        return getSavedFileNames().contains(fileName)
    }

    private fun getSavedFileNames(): Set<String> {
        val names = mutableSetOf<String>()
        val saved = getSavedStatuses()
        for (item in saved) {
            names.add(item.name)
        }
        return names
    }

    fun clearAppCache(): Boolean {
        return try {
            context.cacheDir.deleteRecursively()
            true
        } catch (e: Exception) {
            false
        }
    }
}
