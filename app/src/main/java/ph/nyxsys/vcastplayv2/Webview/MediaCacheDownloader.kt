package ph.nyxsys.vcastplayv2.Webview

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

class MediaCacheDownloader(
    private val context: Context
) {
    fun downloadFile(
        url: String,
        onProgress: (Int) -> Unit,
        onComplete: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        Thread {
            try {
                val fileName = url.substringAfterLast('/')
                val mediaFile = File(context.filesDir, fileName)

                if (mediaFile.exists()) {
                    Log.d("MediaDownload", "File already cached: ${mediaFile.path}")
                    onComplete(mediaFile)
                    return@Thread
                }

                Log.d("MediaDownload", "Downloading file from: $url")
                val urlConnection = URL(url).openConnection()
                val totalSize = urlConnection.contentLength
                val inputStream: InputStream = urlConnection.getInputStream()
                val outputStream = FileOutputStream(mediaFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloaded = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    val progress = if (totalSize > 0) (downloaded * 100 / totalSize) else -1
                    Log.d("MediaProgress", "Downloaded $downloaded / $totalSize bytes ($progress%)")
                    onProgress(progress)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                Log.d("MediaDownload", "Download complete: ${mediaFile.absolutePath}")
                onComplete(mediaFile)
            } catch (e: Exception) {
                Log.e("MediaDownloadError", "Failed: ${e.message}", e)
                onError(e)
            }
        }.start()
    }
}
