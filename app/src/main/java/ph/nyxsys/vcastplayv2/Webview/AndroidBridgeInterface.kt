/**
 * Created by Jam on 2025-08-05.
 *
 */
package ph.nyxsys.vcastplayv2.Webview

import android.app.Activity
import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.net.toUri
import org.json.JSONObject
import ph.nyxsys.vcastplayv2.Utils.DeviceUtil

class AndroidBridgeInterface(
    private val context: Context,
    private val deviceDetails: String,
    private val logHelper: LogHelper,
    private val webView: WebView,
    private val downloadHelper: DownloadHelper
) {
    @JavascriptInterface
    fun getDeviceDetails(): String = deviceDetails

    @JavascriptInterface
    fun receiveDataFromAndroid(): String = deviceDetails

    @JavascriptInterface
    fun onLocalStorageDump(data: String) {
        Log.d("LocalStorageDump", data)
        try {
            val json = JSONObject(data)
            val playerCode = json.optString("playerCode", "UNKNOWN")
            DeviceUtil.playerCode = playerCode
        } catch (e: Exception) {
            Log.e("LocalStorageDump", "Error parsing JSON", e)
        }
    }

    @JavascriptInterface
    fun sendCommand(data: String) {
        if (data != "undefined") {
            Log.e("sendCommand", "From Web: $data")

            try {
                val jsonObject = JSONObject(data)
                val playlistArray = jsonObject.getJSONArray("playlist")

                for (i in 0 until playlistArray.length()) {
                    val item = playlistArray.getJSONObject(i)
                    val link = item.getString("link").trim()
                    val filename = link.toUri().lastPathSegment
                    if (!filename.isNullOrBlank()) {
                        downloadHelper.downloadToLocalStorage(link, filename)
                    }
                }

               // downloadHelper.areAllFilesDownloaded(context, data)
                val allDownloaded = downloadHelper.areAllFilesDownloaded(context, data)

                if (allDownloaded) {

                    (context as? Activity)?.runOnUiThread {
                        webView.evaluateJavascript(
                            """
                    (function waitForFunction() {
                        if (typeof receiveDataFromAndroid === 'function') {
                            receiveDataFromAndroid(${allDownloaded});
                        } else {
                            setTimeout(waitForFunction, 500);
                        }
                    })();
                    """.trimIndent(), null
                        )
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
