package com.shnwaz.lyrasave.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.shnwaz.lyrasave.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {

    private const val GITHUB_API_URL =
        "https://api.github.com/repos/shnwazdeveloper/LyraSave/releases/latest"
    const val CURRENT_VERSION_NAME = "1.0"
    const val CURRENT_VERSION_CODE = 1

    suspend fun checkUpdate(context: Context, isManual: Boolean = false, anchorView: android.view.View? = null) {
        val updateInfo = withContext(Dispatchers.IO) {
            fetchLatestRelease()
        }

        if (updateInfo != null && isNewerVersion(updateInfo.tagName, CURRENT_VERSION_NAME)) {
            showUpdateDialog(context, updateInfo)
        } else if (isManual) {
            val msg = if (updateInfo == null) {
                "Unable to check for updates. Check internet connection."
            } else {
                "Lyra Save is up to date (Version $CURRENT_VERSION_NAME)."
            }
            if (anchorView != null) {
                Snackbar.make(anchorView, msg, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    data class ReleaseInfo(
        val tagName: String,
        val releaseName: String,
        val description: String,
        val downloadUrl: String,
        val releaseUrl: String
    )

    private fun fetchLatestRelease(): ReleaseInfo? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(GITHUB_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "LyraSave-Android")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                val json = JSONObject(response)

                val tagName = json.optString("tag_name", "")
                val releaseName = json.optString("name", tagName)
                val body = json.optString("body", "Bug fixes and improvements.")
                val releaseUrl = json.optString("html_url", "https://github.com/shnwazdeveloper/LyraSave")

                var downloadUrl = releaseUrl
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", true)) {
                            downloadUrl = asset.optString("browser_download_url", releaseUrl)
                            break
                        }
                    }
                }

                ReleaseInfo(
                    tagName = tagName,
                    releaseName = releaseName,
                    description = body,
                    downloadUrl = downloadUrl,
                    releaseUrl = releaseUrl
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = remoteTag.removePrefix("v").trim()
        val cleanCurrent = currentVersion.removePrefix("v").trim()
        if (cleanRemote.isEmpty()) return false
        if (cleanRemote == cleanCurrent) return false

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun showUpdateDialog(context: Context, info: ReleaseInfo) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Update Available")
            .setMessage(
                "A new version (${info.tagName}) of Lyra Save is available.\n\n" +
                "Changes:\n${info.description.ifEmpty { "Bug fixes and improvements." }}"
            )
            .setIcon(R.drawable.ic_app_logo)
            .setPositiveButton("Download Update") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl))
                    context.startActivity(fallbackIntent)
                }
            }
            .setNegativeButton("Later", null)
            .show()
    }
}
