package ph.nyxsys.vcastplayv2.Webview

import android.content.Context
import android.util.Log
import ph.nyxsys.vcastplayv2.Helper.SharedPrefsHelper
import java.io.File

class VideoCacheManager(
    private val context: Context,
    private val sharedPrefs: SharedPrefsHelper,
    private val webViewManager: WebViewManager
) {
    fun downloadVideo(
        url: String,
        onProgress: (Int) -> Unit,
        onComplete: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val downloader = VideoCacheDownloader(context)
        downloader.downloadVideo(
            url = url,
            onProgress = onProgress,
            onComplete = onComplete,
            onError = onError
        )
    }

    fun downloadIfNeeded(
        url: String,
        onProgress: (Int) -> Unit,
        onComplete: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val fileName = url.substring(url.lastIndexOf('/') + 1)
        val cachedFile = File(context.filesDir, fileName)

        if (cachedFile.exists()) {
            Log.d("VideoCacheManager", "Already cached: ${cachedFile.absolutePath}")
            onComplete(cachedFile)
        } else {
            downloadVideo(url, onProgress, onComplete, onError)
        }
    }
}

