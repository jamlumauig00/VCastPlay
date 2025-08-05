/**
 * Created by Jam on 2025-08-05.
 *
 */
package ph.nyxsys.vcastplayv2.Webview

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import ph.nyxsys.vcastplayv2.Utils.DeviceUtil

class WebViewClientImpl(
    private val context: Context,
    private val webView: WebView,
    private val deviceDetails: String,
    private val onPageReady: () -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        Log.e("WebViewError", "Error: ${error?.description}")
        //logoView.visibility = View.VISIBLE
        webView.visibility = View.GONE
        view?.evaluateJavascript("""javascript:displayError("
            |WebView load error: ${error?.description}");""".trimMargin(), null)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
       // logoView.visibility = View.VISIBLE
        webView.visibility = View.GONE
        view?.evaluateJavascript("window.isAndroid = true;", null)
        view?.evaluateJavascript("""
            (function() {
                var data = {};
                for (var i = 0; i < localStorage.length; i++) {
                    var key = localStorage.key(i);
                    data[key] = localStorage.getItem(key);
                }
                AndroidBridge.onLocalStorageDump(JSON.stringify(data));
            })();
        """.trimIndent(), null)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        (context as? FragmentActivity)?.lifecycleScope?.launch {
            val updatedDetails = DeviceUtil.getDeviceDetails(context, context)
            view?.evaluateJavascript("""
                (function waitForFunction() {
                    if (typeof getDeviceDetails === 'function') {
                        getDeviceDetails(${JSONObject.quote(updatedDetails)});
                    } else {
                        setTimeout(waitForFunction, 500);
                    }
                })();
            """.trimIndent(), null)

            onPageReady()
        }
    }
}
