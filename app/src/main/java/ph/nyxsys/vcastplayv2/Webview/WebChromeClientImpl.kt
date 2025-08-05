/**
 * Created by Jam on 2025-08-05.
 *
 */
package ph.nyxsys.vcastplayv2.Webview

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient

class WebChromeClientImpl(
    private val onInitialized: () -> Unit
) : WebChromeClient() {

    override fun onPermissionRequest(request: PermissionRequest?) {
        request?.grant(request.resources)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        Log.d("WebViewConsole", "[${consoleMessage.messageLevel()}] ${consoleMessage.message()}")

        if (consoleMessage.message() == "System has been initialized in ANDROID") {
            onInitialized()
        }
        return true
    }
}
