/**
 * Created by Jam on 2025-08-05.
 *
 */
package ph.nyxsys.vcastplayv2

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import ph.nyxsys.vcastplayv2.Helper.PermissionHelper
import ph.nyxsys.vcastplayv2.Helper.SharedPrefsHelper
import ph.nyxsys.vcastplayv2.Network.ApiClient
import ph.nyxsys.vcastplayv2.Network.QBICShutdown
import ph.nyxsys.vcastplayv2.Utils.*
import ph.nyxsys.vcastplayv2.Webview.WebViewConfigUtil
import ph.nyxsys.vcastplayv2.Webview.WebViewManager
import ph.nyxsys.vcastplayv2.databinding.ActivityMainBinding

enum class SwitchAction {
    CLOSE, OPEN, SHUTDOWN, REOPEN, RESTART
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPrefs: SharedPrefsHelper
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var webViewManager: WebViewManager
    private lateinit var pollingManager: DevicePollingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initSystemSettings()
        initHelpers()
        bindSwitchActions()
        saveSampleCache()
        requestPermissionsAndInit()
    }

    private fun initSystemSettings() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun initHelpers() {
        sharedPrefs = SharedPrefsHelper(this)
        permissionHelper = PermissionHelper(this, 100)
    }

    private fun bindSwitchActions() {
        binding.close.setOnClickListener { handleSwitchAction(SwitchAction.CLOSE) }
        binding.reopen.setOnClickListener { handleSwitchAction(SwitchAction.REOPEN) }
        binding.restart.setOnClickListener { handleSwitchAction(SwitchAction.RESTART) }
        binding.shutdown.setOnClickListener { handleSwitchAction(SwitchAction.SHUTDOWN) }
    }

    private fun saveSampleCache() {
        val cached = DeviceUtil.saveToCache(this, "sample_cache.txt", "cached data".toByteArray())
        Log.d("CacheSave", "File saved: ${cached?.absolutePath}")
    }

    private fun requestPermissionsAndInit() {
        permissionHelper.requestPermissions(
            arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            object : PermissionHelper.PermissionCallback {
                override fun onPermissionGranted() {
                    initWebView()
                    Log.d("Permission", "All permissions granted")
                }

                override fun onPermissionDenied(deniedPermissions: List<String>) {
                    Log.d("Permission", "Denied: $deniedPermissions")
                }
            }
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initWebView() {
        val webView: WebView = binding.webView

        WebViewConfigUtil.configureTransparentTouchWebView(webView) {
            SnackbarUtil.showExitSnackbar(this) { finish() }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val deviceDetails = DeviceUtil.getDeviceDetails(this@MainActivity, this@MainActivity)
            withContext(Dispatchers.Main) {
                webViewManager = WebViewManager(this@MainActivity, webView, deviceDetails)
                webViewManager.setupWebView()
                webViewManager.loadUrl("https://vcastplay-player.vercel.app/")
            }
        }
    }

    private fun handleSwitchAction(action: SwitchAction) {
        when (action) {
            SwitchAction.CLOSE -> finish()
            SwitchAction.OPEN -> relaunchApp()
            SwitchAction.SHUTDOWN -> sendShutdown()
            SwitchAction.REOPEN -> reopenApp()
            SwitchAction.RESTART -> sendReboot()
        }
    }

    @SuppressLint("CheckResult")
    private fun sendShutdown() {
        ApiClient.qbicService.shutdown(QBICShutdown())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { Log.i("QbicControl", "Shutdown command sent.") },
                { Log.e("QbicControl", "Shutdown failed: ${it.message}", it) }
            )
    }

    @SuppressLint("CheckResult")
    private fun sendReboot() {
        ApiClient.qbicService.reboot()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { Log.i("QbicControl", "Reboot command sent.") },
                { Log.e("QbicControl", "Reboot failed: ${it.message}", it) }
            )
    }

    private fun relaunchApp() {
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            startActivity(it)
        } ?: Log.e("AppControl", "Launch intent not found.")
    }

    private fun reopenApp() {
        try {
            val intent = Intent("com.qbic.action.FORCE_STOP_AND_LAUNCH_APP").apply {
                setPackage("com.qbic.systemservice")
                putExtra("packageName", packageName)
            }
            sendBroadcast(intent)
            finish()
        } catch (e: Exception) {
            Log.e("AppControl", "Broadcast failed: ${e.message}", e)
            relaunchApp()
        }
    }

    override fun onStart() {
        super.onStart()
        pollingManager = DevicePollingManager(this, this, binding.deviceDetails)
        pollingManager.startPolling()
    }

    override fun onStop() {
        super.onStop()
        pollingManager.stopPolling()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.handlePermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        ImmersiveUtil.call(window.decorView)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) ImmersiveUtil.call(window.decorView)
    }
}
