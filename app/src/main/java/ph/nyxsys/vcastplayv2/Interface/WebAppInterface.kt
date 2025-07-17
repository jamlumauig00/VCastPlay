package ph.nyxsys.vcastplayv2.Interface

import android.content.Context
import android.webkit.JavascriptInterface
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import ph.nyxsys.vcastplayv2.MainActivity

class WebAppInterface(private val context: Context, private val exoPlayer: ExoPlayer) {

    @JavascriptInterface
    fun playVideo(url: String) {
        (context as MainActivity).runOnUiThread {
            exoPlayer.setMediaItem(MediaItem.fromUri(url))
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }
}