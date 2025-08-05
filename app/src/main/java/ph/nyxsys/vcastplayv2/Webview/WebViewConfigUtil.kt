/**
 * Created by Jam on 2025-08-05.
 *
 */
package ph.nyxsys.vcastplayv2.Webview

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView

object WebViewConfigUtil {

    @SuppressLint("ClickableViewAccessibility")
    fun configureTransparentTouchWebView(webView: WebView, onDoubleTap: () -> Unit) {
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        var lastTapTime = 0L
        webView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val now = System.currentTimeMillis()
                if (now - lastTapTime < 300) {
                    onDoubleTap()
                }
                lastTapTime = now
            }
            false
        }
    }
}
