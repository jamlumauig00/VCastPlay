/**
 * Created by Jam on 2025-08-05.
 *
 */
package ph.nyxsys.vcastplayv2.Webview

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogHelper(private val context: Context) {
    fun logDownload(message: String) {
        try {
            val folder = File(context.getExternalFilesDir(null), "vcastplay 2.0/logs")
            if (!folder.exists()) folder.mkdirs()
            val logFile = File(folder, "download_log.txt")
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val fullMessage = "[$timeStamp] $message\n"
            FileOutputStream(logFile, true).bufferedWriter().use { it.append(fullMessage) }
            Log.d("DownloadLog", fullMessage.trim())
        } catch (e: Exception) {
            Log.e("LogError", "Failed to write log: ${e.message}")
        }
    }
}

