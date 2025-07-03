package ph.nyxsys.vcastplayv2.Webview

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

class VideoCacheDownloader(
    private val context: Context
) {
    fun downloadVideo(
        url: String,
        onProgress: (Int) -> Unit,
        onComplete: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        Thread {
            try {
                val fileName = url.substring(url.lastIndexOf('/') + 1)
                val videoFile = File(context.filesDir, fileName)

                if (videoFile.exists()) {
                    Log.d("Download", "Video already cached: ${videoFile.path}")
                    onComplete(videoFile)
                    return@Thread
                }

                Log.d("Download", "Downloading video from: $url")
                val urlConnection = URL(url).openConnection()
                val totalSize = urlConnection.contentLength
                val inputStream: InputStream = urlConnection.getInputStream()
                val outputStream = FileOutputStream(videoFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloaded = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    val progress = if (totalSize > 0) (downloaded * 100 / totalSize) else -1
                    Log.d("DownloadProgress", "Downloaded $downloaded / $totalSize bytes ($progress%)")
                    onProgress(progress)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                Log.d("Download", "Download complete: ${videoFile.absolutePath}")
                onComplete(videoFile)
            } catch (e: Exception) {
                Log.e("DownloadError", "Failed: ${e.message}")
                onError(e)
            }
        }.start()
    }
}
