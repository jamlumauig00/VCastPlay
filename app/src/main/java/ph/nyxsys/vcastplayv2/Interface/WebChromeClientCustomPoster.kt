package ph.nyxsys.vcastplayv2.Interface

import android.graphics.Bitmap
import android.webkit.WebChromeClient


class WebChromeClientCustomPoster : WebChromeClient() {
    override fun getDefaultVideoPoster(): Bitmap? {
        return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    }
}