package ph.nyxsys.vcastplayv2.Webview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import ph.nyxsys.vcastplayv2.Utils.NetworkUtils

class PollingManager(
    private val context: Context,
    private val webView: WebView,
    private val logoScreen: ImageView,
    private val interval: Long = 5000L
) {
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        handler.post(pollRunnable)
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    private var wasOnline = NetworkUtils.isInternetAvailable(context)

    private val pollRunnable = object : Runnable {
        override fun run() {
            val isOnline = NetworkUtils.isInternetAvailable(context)

            if (!isOnline) {
                logoScreen.visibility = View.VISIBLE
                webView.visibility = View.GONE
            } else {
                if (!wasOnline) {
                    webView.reload()
                }
                logoScreen.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }

            wasOnline = isOnline
            handler.postDelayed(this, interval)
        }
    }

}
