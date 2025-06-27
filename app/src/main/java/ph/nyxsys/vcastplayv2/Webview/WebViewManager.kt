package ph.nyxsys.vcastplayv2.Webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View.*
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import ph.nyxsys.vcastplayv2.Utils.NetworkUtils

class WebViewManager(
    private val context: Context,
    private val webView: WebView,
    private val logoScreen: ImageView,
    private val deviceKey: String
) {
    @SuppressLint("SetJavaScriptEnabled")
    fun setupWebView() {
        webView.settings.apply {
            cacheMode = WebSettings.LOAD_DEFAULT
            domStorageEnabled = true
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            allowUniversalAccessFromFileURLs = true
        }

        webView.addJavascriptInterface(AndroidJavaScriptInterface(deviceKey), "AndroidInterface")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                val description = error?.description ?: "Unknown error"
                if (!NetworkUtils.isInternetAvailable(context)) {
                    webView.settings.cacheMode = WebSettings.LOAD_CACHE_ONLY
                }
                view?.evaluateJavascript("javascript:displayError('WebView load error: $description');", null)
                showLogoScreen()
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                logoScreen.visibility = VISIBLE
                webView.visibility = GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                // You can hook here if needed
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress == 100) {
                    logoScreen.visibility = GONE
                    webView.visibility = VISIBLE
                }
            }
        }
    }

    fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    fun loadVideo(videoPath: String) {
        val videoHtml = """
            <html><body>
            <video autoplay loop playsinline><source src="$videoPath" type="video/mp4"></video>
            </body></html>
        """
        webView.loadDataWithBaseURL("file://", videoHtml, "text/html", "UTF-8", null)
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

    fun showLogoScreen() {
        logoScreen.visibility = VISIBLE
        webView.visibility = GONE
    }

    private class AndroidJavaScriptInterface(private val deviceKey: String) {
        @JavascriptInterface
        fun getDeviceKey() = deviceKey
    }
}
