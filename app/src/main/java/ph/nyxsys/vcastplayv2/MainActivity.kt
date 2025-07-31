package ph.nyxsys.vcastplayv2

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.trusted.TrustedWebActivityIntentBuilder
import androidx.lifecycle.lifecycleScope
import androidx.media3.exoplayer.ExoPlayer
import com.google.androidbrowserhelper.trusted.LauncherActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ph.nyxsys.vcastplayv2.Helper.PermissionHelper
import ph.nyxsys.vcastplayv2.Helper.SharedPrefsHelper
import ph.nyxsys.vcastplayv2.Network.ApiClient
import ph.nyxsys.vcastplayv2.Network.QBICShutdown
import ph.nyxsys.vcastplayv2.Utils.DeviceUtil.getDeviceDetails
import ph.nyxsys.vcastplayv2.Utils.DeviceUtil.saveToCache
import ph.nyxsys.vcastplayv2.Utils.ImmersiveUtil
import ph.nyxsys.vcastplayv2.Webview.MediaCacheManager
import ph.nyxsys.vcastplayv2.Webview.PollingManager
import ph.nyxsys.vcastplayv2.Webview.WebViewManager
import ph.nyxsys.vcastplayv2.databinding.ActivityMainBinding
import ph.nyxsys.vcastplayv2.databinding.LayoutCustomSnackbarBinding
import androidx.core.net.toUri
import androidx.core.graphics.drawable.toDrawable
import com.google.androidbrowserhelper.trusted.TwaLauncher
import kotlinx.coroutines.CoroutineScope


enum class SwitchAction {
    CLOSE, OPEN, SHUTDOWN, REOPEN, RESTART
}
class TwaLauncherActivity : LauncherActivity()

class MainActivity : AppCompatActivity() {
    private val permissionHelper by lazy { PermissionHelper(this, 100) }

    private lateinit var sharedPrefs: SharedPrefsHelper
    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null
    private var deviceDetailsJob: Job? = null
    private lateinit var webViewManager: WebViewManager
    private lateinit var mediaCacheManager: MediaCacheManager
    private lateinit var webView: WebView
    private lateinit var logoScreen: ImageView

    //  private var doubleBackToExitPressedOnce = false
    private lateinit var gestureDetector: GestureDetector
    private var lastTapTime = 0L
    private var snackbarPopup: PopupWindow? = null
    private val TWA_URL = "https://vcastplay-player.vercel.app/"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = SharedPrefsHelper(this)
        webView = binding.webView
        logoScreen = binding.logoScreen
     //   mediaCacheManager = MediaCacheManager(this)

        webViewAction()
        permissionHelper.requestPermissions(
            arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            object : PermissionHelper.PermissionCallback {
                override fun onPermissionGranted() {
                   // webViewAction()
                   /* startTwa2()
                    startDeviceDetailsLoop()
                    logDeviceInfo()*/

                    Log.d("permissions", "All permissions granted")

                }

                override fun onPermissionDenied(deniedPermissions: List<String>) {
                    Log.d("permissions", "Permissions denied: $deniedPermissions")
                }
            }
        )

        binding.close.setOnClickListener {
            handleSwitchAction(SwitchAction.CLOSE)
        }

        binding.reopen.setOnClickListener {
            handleSwitchAction(SwitchAction.REOPEN)
        }

        binding.restart.setOnClickListener {
            handleSwitchAction(SwitchAction.RESTART)
        }

        binding.shutdown.setOnClickListener {
            handleSwitchAction(SwitchAction.SHUTDOWN)
        }

        val textData = "cached data".toByteArray()
        val cacheFile = saveToCache(this, "sample_cache.txt", textData)

        if (cacheFile != null) {
            Log.d("CacheSave", "File saved: ${cacheFile.absolutePath}")
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

       //startTwa2()

        /*gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                finish() // Closes the app

                return true
            }
        })*/
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun webViewAction() {
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        webViewManager = WebViewManager(
            context = this,
            webView = webView,
            logoScreen = logoScreen,
            deviceDetails = "D E V I C E  A N D R O I D" // replace as needed
        )
        webViewManager.setupWebView()
        webViewManager.clearWebViewData(this)
        webViewManager.loadUrl("https://vcastplay-player.vercel.app/")
        //webView.loadUrl("http://172.29.80.1:4200/")
    }


    fun setVideoUrl(url: String) {
        Log.d("Download", url)

        mediaCacheManager.downloadIfNeeded(
            url = url,
            onProgress = { progress -> Log.d("MediaProgress", "$progress% downloaded") },
            onComplete = { file -> Log.d("MediaComplete", "Saved to ${file.absolutePath}") },
            onError = { error -> Log.e("MediaError", error.toString()) }
        )
    }

    private fun showExitSnackBar() {
        if (snackbarPopup?.isShowing == true) return

        val binding = LayoutCustomSnackbarBinding.inflate(LayoutInflater.from(this))

        snackbarPopup = PopupWindow(
            binding.root,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        snackbarPopup?.apply {
            isClippingEnabled = true
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            animationStyle = android.R.style.Animation_Toast

            binding.snackbarAction.setOnClickListener {
                Log.d("showExitSnackBar", "Exit button clicked. Exiting app.")
                dismiss()
                Log.d("showExitSnackBar", "Calling finish() now...")
                (this@MainActivity).finish()
            }

            showAtLocation(findViewById(android.R.id.content), Gravity.BOTTOM, 0, 0)

            binding.root.postDelayed({
                if (isShowing) {
                    Log.d("showExitSnackBar", "showExitSnackBar auto-dismissed after 3 seconds.")
                    dismiss()
                }
            }, 3000)
        }
    }

    private fun handleSwitchAction(action: SwitchAction) {
        when (action) {
            SwitchAction.CLOSE -> closePlayerApp()
            SwitchAction.OPEN -> openPlayerApp()
            SwitchAction.SHUTDOWN -> shutdownDevice()
            SwitchAction.REOPEN -> reopenPlayerApp()
            SwitchAction.RESTART -> triggerReboot()
        }
    }

    @SuppressLint("CheckResult")
    private fun shutdownDevice() {
        ApiClient.qbicService.shutdown(QBICShutdown())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.i("QbicControl", "shutdown command sent successfully.")
            }, { error ->
                Log.e("QbicControl", "shutdown failed: ${error.message}", error)
            })
    }

    private fun logDeviceInfo() {
        Log.d("MainActivity", "Launching TWA with device info...")
    }

    private fun closePlayerApp() {
        Log.d("PlayerControl", "Closing player app.")
        player?.let {
            it.stop()
            it.release()
            player = null
            Log.i("PlayerControl", "Player stopped and released.")
        } ?: Log.w("PlayerControl", "Player not initialized.")
        finish()
    }

    private fun openPlayerApp() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
            Log.i("AppControl", "App relaunched.")
        } else {
            Log.e("AppControl", "Launch intent not found.")
        }
    }

    @SuppressLint("CheckResult")
    private fun triggerReboot() {
        ApiClient.qbicService.reboot()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.i("QbicControl", "Reboot command sent successfully.")
            }, { error ->
                Log.e("QbicControl", "Reboot failed: ${error.message}", error)
            })
    }

    private fun reopenPlayerApp() {
        Log.d("AppControl", "Reopening player via Qbic system broadcast.")
        try {
            val intent = Intent("com.qbic.action.FORCE_STOP_AND_LAUNCH_APP")
            intent.setPackage("com.qbic.systemservice")
            intent.putExtra("packageName", packageName)
            sendBroadcast(intent)
            finish()
        } catch (e: Exception) {
            Log.e("AppControl", "Qbic reopen failed: ${e.message}", e)
            // fallback
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                val restartIntent = Intent.makeRestartActivityTask(launchIntent.component)
                startActivity(restartIntent)
                Runtime.getRuntime().exit(0)
            }
        }
    }


    override fun onStart() {
        super.onStart()
        startDeviceDetailsLoop()
    }

    override fun onStop() {
        super.onStop()
        deviceDetailsJob?.cancel()
    }

    private fun startDeviceDetailsLoop() {
            deviceDetailsJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive) {
                    val details = withContext(Dispatchers.IO) {
                        getDeviceDetails(this@MainActivity, this@MainActivity)
                    }

                    withContext(Dispatchers.Main) {
                        Log.d("DeviceDetails", details)
                        binding.deviceDetails.text = details
                    }

                    delay(5000L)
                }

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.handlePermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onDestroy() {
      //  webView.destroy()  // Avoid memory leaks
        super.onDestroy()
    }


    override fun onResume() {
        super.onResume()
        ImmersiveUtil.call(window.decorView)
//        webView.evaluateJavascript("resumePlayback();", null)
    }

    override fun onPause() {
        super.onPause()
        //webView.evaluateJavascript("pausePlayback();", null)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) ImmersiveUtil.call(window.decorView)

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTime < 300) {
                Log.d("DoubleTap", "Double-tap detected. Showing exit snackbar.")
                showExitSnackBar()
            } else {
                Log.d("DoubleTap", "Single tap detected. Waiting for double tap.")
            }
            lastTapTime = currentTime
        }
        return super.onTouchEvent(event)
    }



    private fun startTwa() {
        val twaLauncher = TwaLauncher(this)
        twaLauncher.launch(TWA_URL.toUri())
    }

    @SuppressLint("UseKtx")
    private fun startTwa2() {
        val builder = TrustedWebActivityIntentBuilder(TWA_URL.toUri())

        val connection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                val session = client.newSession(null)
                if (session != null) {
                    val intent = builder.build(session).intent
                    startActivity(intent)
                } else {
                    Log.e("TWA", "Failed to create Custom Tabs session.")
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {}
        }

        CustomTabsClient.bindCustomTabsService(this, "com.android.chrome", connection)
    }

}
