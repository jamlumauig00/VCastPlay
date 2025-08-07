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
import org.json.JSONArray
import org.json.JSONObject
import ph.nyxsys.vcastplayv2.Utils.DeviceUtil
import java.io.File
import java.net.URL


class AndroidBridgeInterface(
    private val context: Context,
    private val deviceDetails: String,
    private val logHelper: LogHelper,
    private val webView: WebView,
    private val port: Int,
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
        if (data == "undefined") {
            Log.w("sendCommand", "Data is 'undefined', skipping")
            return
        }

        Log.d("sendCommand", "From Web: $data")

        try {
            val jsonObject = JSONObject(data)
            val playlistArray = jsonObject.optJSONArray("playlist")

            if (playlistArray == null || playlistArray.length() == 0) {
                Log.w("sendCommand", "Playlist is empty or invalid")
                return
            }

            val mediaDir = File(context.filesDir, "medias/")
            if (!mediaDir.exists()) {
                val created = mediaDir.mkdirs()
                Log.i("sendCommand", "mediaDir created: $created")
            }

            Log.i("sendCommand", "mediaDir: $mediaDir")

            val updatedPlaylist = JSONArray()

            for (i in 0 until playlistArray.length()) {
                val item = playlistArray.getJSONObject(i)
                val link = item.optString("link").trim()
                val filename = link.toUri().lastPathSegment

                if (filename.isNullOrEmpty()) {
                    Log.w("sendCommand", "Skipped item with empty filename: $link")
                    continue
                }

                val file = File(mediaDir, filename)
                Log.d("sendCommand", "Checking file: ${file.absolutePath}")

                if (!file.exists()) {
                    try {
                        val connection = URL(link).openConnection()
                        connection.connect()
                        file.outputStream().use { output ->
                            connection.getInputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        Log.i("sendCommand", "Downloaded: $filename")
                    } catch (e: Exception) {
                        Log.e("sendCommand", "Failed to download $link", e)
                        continue
                    }
                } else {
                    Log.d("sendCommand", "Already exists: $filename")
                }

                // Replace link with local path and add once to the array
                item.put("link", "https://appassets.androidplatform.net/medias/$filename")
                updatedPlaylist.put(item)
            }

            (context as? Activity)?.runOnUiThread {
                val arrayStr = updatedPlaylist.toString()
                val escapedArray = JSONObject.quote(arrayStr)

                webView.evaluateJavascript("window.receiveDataFromAndroid(JSON.parse($escapedArray));", null)
                Log.d("sendCommand", "Final Array sent to JS: $arrayStr")

            }

            /*   val resultJson = JSONObject().apply {
                   put("playlist", updatedPlaylist)
               }

               // Safely escape and send to JS
               val escapedJson = JSONObject.quote(resultJson.toString())
               Log.d("BridgeToWeb", "Sending to JS1: ${updatedPlaylist.put(item)")

               (context as? Activity)?.runOnUiThread {
                   webView.evaluateJavascript("window.receiveDataFromAndroid($resultJson);", null)
               }
   */
        } catch (e: Exception) {
            Log.e("sendCommand", "Error processing playlist", e)
        }
    }

    @JavascriptInterface
    fun checkFileExists(virtualUrl: String): Boolean {
        Log.e("checkFileExists", "checkFileExists:  $virtualUrl")

        val baseUrl = "https://appassets.androidplatform.net/medias/"

        val exists = if (virtualUrl.startsWith(baseUrl)) {
            val filename = virtualUrl.removePrefix(baseUrl)
            val file = File(context.filesDir, "medias/$filename")
            val fileExist = file.exists()
            Log.e("checkFileExists", "checkFileExists:  $fileExist")
            fileExist
        } else {
            false
        }

        Log.e("checkFileExists", "Returning to JS: $exists")

        // ❗ No need to call back to JS manually (evaluateJavascript)
        return exists
    }
}

