package ph.nyxsys.vcastplayv2.Webview

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.launch
import org.json.JSONObject
import ph.nyxsys.vcastplayv2.Utils.DeviceUtil
import java.io.File
import androidx.core.net.toUri

class WebViewClientImpl(
    private val context: Context,
    private val webView: WebView,
    private val deviceDetails: String,
    private val onPageReady: () -> Unit
) : WebViewClient() {

    private val assetLoader: WebViewAssetLoader = WebViewAssetLoader.Builder()
        .addPathHandler(
            "/medias/",
            WebViewAssetLoader.InternalStoragePathHandler(context, File(context.filesDir, "medias"))
        )
        .build()

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return false
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val intercepted = assetLoader.shouldInterceptRequest(request.url)
        if (intercepted != null) {
            Log.d("WebViewClientImpl", "Intercepted: ${request.url}")
        }
        return intercepted
    }

    @Deprecated("Deprecated in Java")
    override fun shouldInterceptRequest(view: WebView?, url: String): WebResourceResponse? {
        return assetLoader.shouldInterceptRequest(url.toUri())
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        val errorMessage = error?.description ?: "Unknown error"
        Log.e("WebViewClientImpl", "WebView error: $errorMessage")

        webView.visibility = View.GONE
        view?.evaluateJavascript(
            """javascript:displayError("WebView load error: $errorMessage");""",
            null
        )
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        webView.visibility = View.GONE
        view?.evaluateJavascript("window.isAndroid = true;", null)

        // Send localStorage contents to Android (optional)
        view?.evaluateJavascript(
            """
            (function() {
                var data = {};
                for (var i = 0; i < localStorage.length; i++) {
                    var key = localStorage.key(i);
                    data[key] = localStorage.getItem(key);
                }
                if (AndroidBridge && AndroidBridge.onLocalStorageDump)
                    AndroidBridge.onLocalStorageDump(JSON.stringify(data));
            })();
            """.trimIndent(),
            null
        )
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        (context as? FragmentActivity)?.lifecycleScope?.launch {
            try {
                val updatedDetails = DeviceUtil.getDeviceDetails(context, context)

                view?.evaluateJavascript(
                    """
                    (function waitForFunction() {
                        if (typeof getDeviceDetails === 'function') {
                            getDeviceDetails(${JSONObject.quote(updatedDetails)});
                        } else {
                            setTimeout(waitForFunction, 500);
                        }
                    })();
                    """.trimIndent(),
                    null
                )
            } catch (e: Exception) {
                Log.e("WebViewClientImpl", "Failed to inject device details", e)
            } finally {
                onPageReady()
            }
        }
    }
}
