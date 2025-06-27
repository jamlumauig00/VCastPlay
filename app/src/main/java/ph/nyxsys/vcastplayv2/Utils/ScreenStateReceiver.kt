package ph.nyxsys.vcastplayv2.Utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScreenStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_OFF -> Log.d("ScreenState", "Screen is OFF")
            Intent.ACTION_SCREEN_ON -> Log.d("ScreenState", "Screen is ON")
        }
    }
}

