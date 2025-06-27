package ph.nyxsys.vcastplayv2.Webview

import android.content.Context
import android.util.Log
import ph.nyxsys.vcastplayv2.Helper.SharedPrefsHelper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

class VideoCacheManager(
    private val context: Context,
    private val sharedPrefsHelper: SharedPrefsHelper,
    private val webViewManager: WebViewManager
) {
    fun downloadVideo(url: String) {
        Thread {
            try {
                val fileName = url.substring(url.lastIndexOf('/') + 1)
                val videoFile = File(context.filesDir, fileName)

                if (videoFile.exists()) {
                    Log.d("Download", "Video already cached: ${videoFile.path}")
                    webViewManager.loadVideo("file://${videoFile.path}")
                    return@Thread
                }

                val urlConnection = URL(url).openConnection()
                val inputStream: InputStream = urlConnection.getInputStream()
                val outputStream = FileOutputStream(videoFile)

                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.close()
                inputStream.close()

                Log.d("Download", "Download complete.")
                sharedPrefsHelper.setLastPlayed(url)
                webViewManager.loadVideo("file://${videoFile.path}")
            } catch (e: Exception) {
                Log.e("DownloadError", "Failed: ${e.message}")
                webViewManager.showLogoScreen()
            }
        }.start()
    }
}
