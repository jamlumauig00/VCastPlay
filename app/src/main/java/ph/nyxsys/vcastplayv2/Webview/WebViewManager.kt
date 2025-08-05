/**
 * Created by Jam on 2025-08-05.
 *
 */
package ph.nyxsys.vcastplayv2.Webview

import android.content.Context
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.ImageView
import ph.nyxsys.vcastplayv2.Utils.NetworkUtils
import java.io.File

class WebViewManager(
    private val context: Context,
    private val webView: WebView,
    private val deviceDetails: String
) {

    private val downloadHelper = DownloadHelper(context)
    private val logHelper = LogHelper(context)

    fun setupWebView() {
        WebViewConfigurator.configure(webView)
        WebView.setWebContentsDebuggingEnabled(true)

        webView.addJavascriptInterface(
            AndroidBridgeInterface(
                context,
                deviceDetails,
                logHelper,
                webView,
                downloadHelper
            ), "AndroidBridge"
        )
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun resumePlayback() {
                Log.d("JSInterface", "resumePlayback called")
            }
        }, "Android")

        webView.webChromeClient = WebChromeClientImpl { showWeb() }
        webView.webViewClient =
            WebViewClientImpl(context, webView, deviceDetails) { showWeb() }
    }

    fun loadUrl(url: String) {
        val offlineFile = File(context.cacheDir, "offline.html")
        if (NetworkUtils.isInternetAvailable(context)) {
            webView.loadUrl(url)
            downloadHelper.downloadHtmlPage(url, offlineFile) {
                Log.d("WebViewManager", "Page downloaded to ${offlineFile.path}")
            }
        } else if (offlineFile.exists()) {
            webView.loadUrl("file://${offlineFile.absolutePath}")
        } else {
            Log.e("WebViewManager", "Offline file not found.")
            showLogo()
        }
    }

    fun clearCache() {
        context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        webView.clearCache(true)
    }

    private fun showLogo() {
       // logoScreen.visibility = VISIBLE
        webView.visibility = GONE
    }

    private fun showWeb() {
       // logoScreen.visibility = GONE
        webView.visibility = VISIBLE
    }
}

