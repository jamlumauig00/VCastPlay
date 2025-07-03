package ph.nyxsys.vcastplayv2

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.material.snackbar.Snackbar
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
import ph.nyxsys.vcastplayv2.Utils.DeviceUtil
import ph.nyxsys.vcastplayv2.Utils.DeviceUtil.getDeviceDetails
import ph.nyxsys.vcastplayv2.Utils.DeviceUtil.saveToCache
import ph.nyxsys.vcastplayv2.Utils.ImmersiveUtil
import ph.nyxsys.vcastplayv2.Utils.NetworkUtils
import ph.nyxsys.vcastplayv2.Webview.PollingManager
import ph.nyxsys.vcastplayv2.Webview.VideoCacheManager
import ph.nyxsys.vcastplayv2.Webview.WebViewManager
import ph.nyxsys.vcastplayv2.databinding.ActivityMainBinding
import ph.nyxsys.vcastplayv2.databinding.LayoutCustomSnackbarBinding

enum class SwitchAction {
    CLOSE, OPEN, SHUTDOWN, REOPEN, RESTART
}

class MainActivity : AppCompatActivity() {
    private val permissionHelper by lazy { PermissionHelper(this, 100) }

    private lateinit var sharedPrefs: SharedPrefsHelper
    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null
    private var deviceDetailsJob: Job? = null
    private lateinit var webViewManager: WebViewManager
    private lateinit var pollingManager: PollingManager
    private lateinit var videoCacheManager: VideoCacheManager
    private lateinit var webView: WebView
    private lateinit var logoScreen: ImageView

  //  private var doubleBackToExitPressedOnce = false
   // private lateinit var gestureDetector: GestureDetector

    private var lastTapTime = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = SharedPrefsHelper(this)
        webView = binding.webView
        logoScreen = binding.logoScreen

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
                    Toast.makeText(this@MainActivity, "All permissions granted", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onPermissionDenied(deniedPermissions: List<String>) {
                    Toast.makeText(
                        this@MainActivity,
                        "Permissions denied: $deniedPermissions",
                        Toast.LENGTH_SHORT
                    ).show()
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


        /*gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                showExitSnackbar()
                return true
            }
        })*/
    }

    private fun webViewAction() {
        webViewManager = WebViewManager(this, webView, logoScreen, "device1")
        pollingManager = PollingManager(this, webView, logoScreen)
        videoCacheManager = VideoCacheManager(this, sharedPrefs, webViewManager)

        webViewManager.setupWebView()

        if (NetworkUtils.isInternetAvailable(this)) {
            Log.d("WebView", "Internet available: loading remote URL")
            webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewManager.loadUrl("file:///android_asset/index.html")
        } else {
            Log.w("WebView", "No internet: loading local asset fallback")
            webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewManager.loadUrl("file:///android_asset/index.html")
        }


      //  webViewManager.loadUrl("file:///android_asset/index.html")
        pollingManager.start()
    }

    fun setVideoUrl(url: String) {
        if (!url.endsWith(".mp4", ignoreCase = true) &&
            !url.endsWith(".webm", ignoreCase = true) &&
            !url.endsWith(".ogg", ignoreCase = true)) {
            Log.d("Download", "Skipping non-video URL: $url")
            return
        }

        videoCacheManager.downloadIfNeeded(
            url = url,
            onProgress = { progress ->
                Log.d("Download", "Progress: $progress%")
            },
            onComplete = { file ->
                Log.d("Download", "Cached at: ${file.absolutePath}")
            },
            onError = { error ->
                Log.e("Download", "Error: ${error.message}")
            }
        )
    }
    private fun showExitSnackbar() {
        Log.d("Snackbar", "Showing exit snackbar...")

        val binding = LayoutCustomSnackbarBinding.inflate(LayoutInflater.from(this))

        val popup = PopupWindow(
            binding.root,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popup.isClippingEnabled = false
        popup.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.animationStyle = android.R.style.Animation_Toast

        binding.snackbarAction.setOnClickListener {
            Log.d("Snackbar", "Exit button clicked. Exiting app.")
            //PlayerView.playerIsPlaying = false
            popup.dismiss()
            finish()
        }

        popup.showAtLocation(findViewById(android.R.id.content), Gravity.BOTTOM, 0, 0)

        binding.root.postDelayed({
            if (popup.isShowing) {
                Log.d("Snackbar", "Snackbar auto-dismissed after 3 seconds.")
                popup.dismiss()
            }
        }, 3000)
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
        deviceDetailsJob = lifecycleScope.launch {
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            permissionHelper.handlePermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onDestroy() {
        pollingManager.stop()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        ImmersiveUtil.call(window.decorView)
        webView.evaluateJavascript("resumePlayback();", null)
    }

    override fun onPause() {
        super.onPause()
        webView.evaluateJavascript("pausePlayback();", null)
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
                showExitSnackbar()
            } else {
                Log.d("DoubleTap", "Single tap detected. Waiting for double tap.")
            }
            lastTapTime = currentTime
        }
        return super.onTouchEvent(event)
    }



}
