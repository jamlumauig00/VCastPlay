package ph.nyxsys.vcastplayv2.Webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.webkit.*
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import ph.nyxsys.vcastplayv2.Interface.WebChromeClientCustomPoster
import ph.nyxsys.vcastplayv2.MainActivity
import ph.nyxsys.vcastplayv2.Utils.DeviceUtil
import ph.nyxsys.vcastplayv2.Utils.NetworkUtils
import java.io.File
import java.net.URL

class WebViewManager(
    private val context: Context,
    private val webView: WebView,
    private val logoScreen: ImageView,
    private val deviceDetails: String
) {

    @SuppressLint("SetJavaScriptEnabled")
    fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            allowUniversalAccessFromFileURLs = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            setSupportMultipleWindows(false)
            cacheMode = WebSettings.LOAD_NO_CACHE
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.addJavascriptInterface(AndroidJavaScriptInterface(), "AndroidBridge")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d(
                    "WebViewConsole",
                    "[${consoleMessage.messageLevel()}] ${consoleMessage.message()} " +
                            "Line: ${consoleMessage.lineNumber()} Source: ${consoleMessage.sourceId()}"
                )

                if (consoleMessage.message() == "System has been initialized in ANDROID") {
                    showWeb()
                }

                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Log.e("WebViewError", "Error: ${error?.description ?: "Unknown error"}")
                showLogo()
                view?.evaluateJavascript(
                    """javascript:displayError("WebView load error: ${error?.description}");""",
                    null
                )
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                showLogo()
                val safeDeviceDetails = JSONObject.quote(deviceDetails)

                view?.evaluateJavascript("window.isAndroid = true;", null)
                view?.evaluateJavascript("javascript:getDeviceDetails($safeDeviceDetails);", null)

                val localStorageDumpScript = """
                    (function() {
                        var data = {};
                        for (var i = 0; i < localStorage.length; i++) {
                            var key = localStorage.key(i);
                            data[key] = localStorage.getItem(key);
                        }
                        AndroidBridge.onLocalStorageDump(JSON.stringify(data));
                    })();
                """
                view?.evaluateJavascript(localStorageDumpScript, null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d("WebView", "Page loaded: $url")
                webView.visibility = VISIBLE
                super.onPageFinished(view, url)
                /*val removePosterJs = """
                        (function() {
                            const video = document.querySelector('video');
                            if (video) {
                                video.removeAttribute('poster');
                                video.poster = "";
                                video.style.backgroundImage = "none";
                            }
                    
                            const posterOverlay = document.querySelector('.vjs-poster');
                            if (posterOverlay) {
                                posterOverlay.style.display = 'none';
                                console.log("✅ vjs-poster hidden");
                            } else {
                                console.log("⚠️ vjs-poster not found");
                            }
                        })();
                    """.trimIndent()

                    webView.evaluateJavascript(removePosterJs, null)*/

                (context as? FragmentActivity)?.lifecycleScope?.launch {
                    val updatedDetails = DeviceUtil.getDeviceDetails(context, context)
                    val safeDeviceDetails = JSONObject.quote(updatedDetails)

                    val js = """
                        setTimeout(function() {
                            if (typeof getDeviceDetails === 'function') {
                                getDeviceDetails($safeDeviceDetails);
                            } else {
                                console.warn("getDeviceDetails is not ready.");
                            }
                        }, 0);
                    """.trimIndent()

                    view?.evaluateJavascript(js, null)
                    showWeb()
                }
            }
        }
    }

    fun loadUrl(url: String) {
        val file = File(context.cacheDir, "offline.html")

        if (NetworkUtils.isInternetAvailable(context)) {
            webView.loadUrl(url)
            downloadHtmlPage(url, file) {
                Log.d("WebViewManager", "Page downloaded: $url to ${file.path}")
            }
        } else {
            if (file.exists()) {
                webView.loadUrl("file://${file.absolutePath}")
            } else {
                Log.e("WebViewManager", "Offline file not found.")
                showLogo()
            }
        }
    }

    private fun downloadHtmlPage(url: String, saveTo: File, onComplete: () -> Unit) {
        Thread {
            try {
                val html = URL(url).readText()
                saveTo.writeText(html)
                onComplete()
            } catch (e: Exception) {
                Log.e("DownloadHtml", "Error: ${e.message}")
            }
        }.start()
    }

    fun clearCache() {
        cacheDirCleanup()
        webView.clearCache(true)
    }

    private fun cacheDirCleanup() {
        try {
            context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        } catch (e: Exception) {
            Log.e("CacheError", "Error clearing cache: ${e.message}")
        }
    }

    fun showLogo() {
        logoScreen.visibility = VISIBLE
        webView.visibility = GONE
    }

    fun showWeb() {
        logoScreen.visibility = GONE
        webView.visibility = VISIBLE
    }

    inner class AndroidJavaScriptInterface {

        @JavascriptInterface
        fun getDeviceDetails(): String = deviceDetails

        @JavascriptInterface
        fun onLocalStorageDump(data: String) {
            Log.d("LocalStorageDump", data)

            try {
                val json = JSONObject(data)
                val playerCode = json.optString("playerCode", "UNKNOWN")
                Log.d("ParsedPlayerCode", playerCode)
                DeviceUtil.playerCode = playerCode
            } catch (e: Exception) {
                Log.e("LocalStorageDump", "Error parsing JSON", e)
            }
        }

        @JavascriptInterface
        fun sendCommand(data: String) {
            Log.d("WebViewData", "Command from web: $data")
            if (data != "undefined") {
                Toast.makeText(context, "From Web: $data", Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun setVideoUrl(url: String) {
            Log.d("AndroidInterface", "Video URL from JS: $url")
            (context as? MainActivity)?.setVideoUrl(url)
        }
    }
}
