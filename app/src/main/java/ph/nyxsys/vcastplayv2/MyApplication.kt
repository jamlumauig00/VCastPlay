package ph.nyxsys.vcastplayv2

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import com.google.firebase.FirebaseApp
import ph.nyxsys.vcastplayv2.Utils.ScreenStateReceiver

class MyApplication : Application() {
    private lateinit var screenStateReceiver: ScreenStateReceiver

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Register the screen state receiver globally
        screenStateReceiver = ScreenStateReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, filter)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val flags = View.SYSTEM_UI_FLAG_IMMERSIVE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

            // Apply immersive flags to every activity
            val activityLifecycleCallbacks = object : ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    activity.window.decorView.systemUiVisibility = flags
                }

                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            }

            // Register the activity lifecycle callback
            registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        }

    }


    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(screenStateReceiver)
    }
}
