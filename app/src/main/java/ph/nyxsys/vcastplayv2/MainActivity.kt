package ph.nyxsys.vcastplayv2

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.exoplayer.ExoPlayer
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
import ph.nyxsys.vcastplayv2.databinding.ActivityMainBinding


enum class SwitchAction {
    CLOSE, OPEN, SHUTDOWN, REOPEN, RESTART
}

class MainActivity : AppCompatActivity() {
    private val permissionHelper by lazy { PermissionHelper(this, 100) }

    private lateinit var sharedPrefs: SharedPrefsHelper
    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null
    private var deviceDetailsJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = SharedPrefsHelper(this)

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


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.handlePermissionsResult(requestCode, permissions, grantResults)
    }

}
