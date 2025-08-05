/**
 * Created by Jam on 2025-08-05.
 *
 */
package ph.nyxsys.vcastplayv2.Webview

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DownloadHelper(private val context: Context) {
    fun downloadHtmlPage(url: String, saveTo: File, onComplete: () -> Unit) {
        Thread {
            try {
                saveTo.writeText(URL(url).readText())
                onComplete()
            } catch (e: Exception) {
                Log.e("DownloadHtml", "Error: ${e.message}")
            }
        }.start()
    }

    fun downloadToLocalStorage(url: String, fileName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val folder = File(context.getExternalFilesDir(null), "vcastplay 2.0/assets/medias")
                if (!folder.exists()) folder.mkdirs()

                val file = File(folder, fileName)

                val connection = URL(url).openConnection()
                connection.connect()

                val remoteFileSize = connection.contentLengthLong

                // Skip if file is already up-to-date
                if (file.exists() && file.length() == remoteFileSize) {
                    logDownloadStatus("⏩ Skipped (Already up-to-date): $fileName")
                    return@launch
                }

                val input = connection.getInputStream()
                val output = FileOutputStream(file)

                val buffer = ByteArray(4096)
                var totalRead = 0L
                var bytesRead: Int
                var lastLoggedPercent = -1

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    if (remoteFileSize > 0) {
                        val percent = ((totalRead * 100) / remoteFileSize).toInt()
                        if (percent != lastLoggedPercent && percent % 10 == 0) {
                            logDownloadStatus("📦 Downloading $fileName: $percent%")
                            lastLoggedPercent = percent
                        }
                    }
                }

                input.close()
                output.close()

                logDownloadStatus("✅ Downloaded: $fileName → ${file.absolutePath}")
            } catch (e: Exception) {
                logDownloadStatus("❌ Failed to download $fileName\nError: ${e.message}")
            }
        }
    }

    private fun logDownloadStatus(message: String) {
        try {
            val logFolder = File(context.getExternalFilesDir(null), "vcastplay 2.0/logs")
            if (!logFolder.exists()) logFolder.mkdirs()

            val logFile = File(logFolder, "download_log.txt")

            val timeStamp =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val fullMessage = "[$timeStamp] $message\n"

            FileOutputStream(logFile, true).bufferedWriter().use { writer ->
                writer.append(fullMessage)
            }

            Log.d("DownloadLog", fullMessage.trim())
        } catch (e: Exception) {
            Log.e("LogError", "Failed to write log: ${e.message}", e)
        }
    }

    fun areAllFilesDownloaded(context: Context, jsonString: String): Boolean {
        val mediaFolder = File(context.getExternalFilesDir(null), "vcastplay 2.0/assets/medias")
        if (!mediaFolder.exists()) mediaFolder.mkdirs()

        return try {
            val playlist = JSONObject(jsonString).getJSONArray("playlist")

            for (i in 0 until playlist.length()) {
                val item = playlist.getJSONObject(i)
                val url = item.getString("link").trim()
                val fileName = url.toUri().lastPathSegment ?: continue
                val localFile = File(mediaFolder, fileName)

                // 1. File existence check
                if (!localFile.exists()) {
                    Log.d("FileCheck", "❌ Missing: $fileName")
                    return false
                }

                val remoteSize = URL(url).openConnection().apply { connect() }.contentLengthLong
                if (remoteSize > 0 && localFile.length() != remoteSize) {
                    Log.d("FileCheck", "❌ Incomplete or outdated: $fileName")
                    return false
                }

                Log.d("FileCheck", "✅ Present: $fileName")
            }

            true // All files exist and are valid


        } catch (e: Exception) {
            Log.e("FileCheck", "Error: ${e.message}")
            false
        }
    }


}

