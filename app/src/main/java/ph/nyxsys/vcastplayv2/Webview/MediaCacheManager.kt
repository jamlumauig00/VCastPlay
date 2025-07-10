package ph.nyxsys.vcastplayv2.Webview

import android.content.Context
import android.util.Log
import java.io.File

class MediaCacheManager(
    private val context: Context,
) {
    fun downloadFile(
        url: String,
        onProgress: (Int) -> Unit,
        onComplete: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val downloader = MediaCacheDownloader(context)
        downloader.downloadFile(
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
        val fileName = url.substringAfterLast('/')
        val cachedFile = File(context.filesDir, fileName)

        if (cachedFile.exists()) {
            Log.d("MediaCacheManager", "Already cached: ${cachedFile.absolutePath}")
            onComplete(cachedFile)
        } else {
            downloadFile(url, onProgress, onComplete, onError)
        }
    }
}
