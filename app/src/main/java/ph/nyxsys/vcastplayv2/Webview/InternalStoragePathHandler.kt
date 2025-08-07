package ph.nyxsys.vcastplayv2.Webview

import android.content.Context
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileInputStream

class InternalStoragePathHandler(private val context: Context) :
    WebViewAssetLoader.PathHandler {

    override fun handle(path: String): WebResourceResponse? {
        val file = File(context.filesDir, "medias/$path")
        if (!file.exists()) return null

        val mimeType = when {
            path.endsWith(".jpg", true) || path.endsWith(".jpeg", true) -> "image/jpeg"
            path.endsWith(".png", true) -> "image/png"
            path.endsWith(".mp4", true) -> "video/mp4"
            else -> "application/octet-stream"
        }

        return WebResourceResponse(
            mimeType,
            "UTF-8",
            FileInputStream(file)
        )
    }
}
