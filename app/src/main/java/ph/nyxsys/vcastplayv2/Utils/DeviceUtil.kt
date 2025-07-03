package ph.nyxsys.vcastplayv2.Utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.Settings
import android.util.Log
import android.view.Display
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okio.IOException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL
import java.security.SecureRandom
import java.util.*
import javax.net.ssl.HttpsURLConnection

object DeviceUtil {

    private const val LETTER_COUNT = 5
    private const val NUMBER_COUNT = 5
    private val LETTER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()
    private val NUMBER_CHARS = "0123456789".toCharArray()
    private var cachedDeviceId: String? = null

    fun playAudio(context: Context, name: String, mediaPlayerList: ArrayList<MediaPlayer>) {
        val descriptor = context.assets.openFd(name)
        val start = descriptor.startOffset
        val end = descriptor.length

        mediaPlayerList.forEach {
            try {
                it.stop()
                it.release()
            } catch (_: Exception) {
            }
        }
        mediaPlayerList.clear()

        val player = MediaPlayer()
        player.setDataSource(descriptor.fileDescriptor, start, end)
        player.prepare()
        player.start()

        player.setOnCompletionListener { it.release() }
        player.setOnErrorListener { mp, _, _ -> mp.release(); true }

        mediaPlayerList.add(player)
    }

    fun installAPK(filename: String) {
        val file = File(filename)
        if (file.exists()) {
            try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "pm install -r $filename")).waitFor()
                file.delete()
                Log.e("installingAPK", filename)
            } catch (e: Exception) {
                Log.e("Failed to install", e.toString())
            }
        }
    }

    fun getIPAddress(): String? {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        if (sAddr.indexOf(':') < 0) return sAddr // IPv4 only
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun getCpuTemp(): Float {
        val cpuFiles = arrayOf(
            "cat sys/class/thermal/thermal_zone0/temp",
            "cat sys/class/thermal/thermal_zone1/temp",
            "cat sys/devices/system/cpu/cpu0/cpufreq/cpu_temp"
        )
        return try {
            for (cmd in cpuFiles) {
                val p = Runtime.getRuntime().exec(cmd)
                p.waitFor()
                val reader = BufferedReader(InputStreamReader(p.inputStream))
                val line = reader.readLine()
                reader.close()
                if (!line.isNullOrBlank()) return line.toFloat() / 1000
            }
            30.0f
        } catch (e: Exception) {
            e.printStackTrace()
            30.0f
        }
    }

    private fun formatSize(bytes: Long): String {
        var size = bytes
        val suffix = when {
            size >= 1024 * 1024 -> {
                size /= (1024 * 1024); "MB"
            }

            size >= 1024 -> {
                size /= 1024; "KB"
            }

            else -> null
        }
        return buildString {
            append(size)
            suffix?.let { append(it) }
        }
    }

    fun getAvailableInternalStorageSize(path: String): String {
        return try {
            val stat = StatFs(File(path).path)
            formatSize(stat.availableBlocksLong * stat.blockSizeLong)
        } catch (e: Exception) {
            "N/A"
        }
    }

    fun getTotalInternalStorageSize(path: String): String {
        return try {
            val stat = StatFs(File(path).path)
            formatSize(stat.blockCountLong * stat.blockSizeLong)
        } catch (e: Exception) {
            "N/A"
        }
    }

    fun getRamSize(context: Context): String {
        val mi = ActivityManager.MemoryInfo()
        val am = context.getSystemService(Service.ACTIVITY_SERVICE) as ActivityManager
        am.getMemoryInfo(mi)
        val available = mi.availMem / 0x100000L
        val total = mi.totalMem / 0x100000L
        return "$available MB of $total MB"
    }

    fun isNetworkConnectionAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }

    fun getNetworkStatus(context: Context): String {
        val isNetworkConnectionAvailable = isNetworkConnectionAvailable(context)
        return if (isNetworkConnectionAvailable) {
            "Online"
        } else "Offline"
    }

    fun getScreenResolution(activity: FragmentActivity?): String {
        val display = activity?.windowManager?.defaultDisplay ?: return "Unknown"
        val size = Point()
        display.getRealSize(size)
        return "${size.x} x ${size.y}"
    }

    fun getScreenSize(displayManager: DisplayManager): Point {
        val size = Point()
        displayManager.displays.firstOrNull()?.getRealSize(size)
        if (size.x < 1280 || size.y < 720) {
            size.x = 1920
            size.y = 1080
        }
        return size
    }

    fun isVideoOrImage(value: String): Boolean {
        val ext = value.lowercase(Locale.ROOT)
        return ext.endsWith(".mp4") || ext.endsWith(".mov") || ext.endsWith(".wmv") || ext.endsWith(
            ".avi"
        ) || ext.endsWith(
            ".png"
        ) || ext.endsWith(".jpg") || ext.endsWith(".jpeg") || ext.endsWith(".gif") || ext.endsWith(".mpeg") || ext.endsWith(
            ".svg"
        )
    }

    fun shutdownDevice() {
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot -p")).waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun rebootDevice() {
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot")).waitFor()
        } catch (e: Exception) {
            Log.e("RestartHere", e.toString())
        }
    }

    fun lockOrientation(activity: FragmentActivity) {
        activity.requestedOrientation =
            if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    fun unlockOrientation(activity: FragmentActivity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    }

    fun setVolumeToMax(context: Context?) {
        val am = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        am.setStreamVolume(AudioManager.STREAM_MUSIC, 50, 0)
    }

    fun setVolumeToMute(context: Context?) {
        val am = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
    }

    @SuppressLint("MissingPermission")
    fun getBuildSerial(context: Context): String {
        val sharedPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        return try {
            val stored = sharedPrefs.getString("build_serial", null)
            val serial = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> null
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    if (ActivityCompat.checkSelfPermission(
                            context, Manifest.permission.READ_PHONE_STATE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) Build.getSerial() else null
                }

                else -> Build.SERIAL
            }?.takeIf { it.isNotBlank() } ?: getDeviceId()

            if (stored.isNullOrEmpty()) {
                storeInSharedPreferences(context, serial, "first_time_store")
                return serial
            }

            if (stored != serial) {
                writeSerialNumberToFile(stored)
                return stored
            }

            stored
        } catch (e: Exception) {
            //Log.e("getBuildSerial", "Error: ${e}", e)
            sharedPrefs.getString("build_serial", null) ?: getDeviceId().also {
                storeInSharedPreferences(context, it, "exception_fallback")
            }
        }
    }

    private fun getDeviceId(): String {
        cachedDeviceId?.let { return it }
        val stored = readSerialNumberFromFile()
        if (stored != null) {
            cachedDeviceId = stored
            return stored
        }
        val newId = "NYX${generateRandomId()}"
        writeSerialNumberToFile(newId)
        cachedDeviceId = newId
        return newId
    }

    private fun generateRandomId(): String {
        val random = SecureRandom()
        val letters = CharArray(LETTER_COUNT) { LETTER_CHARS[random.nextInt(LETTER_CHARS.size)] }
        val numbers = CharArray(NUMBER_COUNT) { NUMBER_CHARS[random.nextInt(NUMBER_CHARS.size)] }
        return (letters + numbers).toList().shuffled().joinToString("")
    }

    private fun storeInSharedPreferences(context: Context, value: String, reason: String = "") {
        Log.i("SerialStorage", "Storing serial: $value due to $reason")
        context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit()
            .putString("build_serial", value).apply()
    }

    private fun writeSerialNumberToFile(serialNumber: String) {
        getSerialNumberFilePath().writeText("serialNumber = \"$serialNumber\"")
    }

    private fun readSerialNumberFromFile(): String? {
        val file = getSerialNumberFilePath()
        return if (file.exists()) {
            file.readText().substringAfter("serialNumber = \"").substringBefore("\"")
        } else null
    }

    private fun getSerialNumberFilePath(): File {
        return File("/storage/emulated/0/", "SerialNumber.txt")
    }

    fun getInternalStorageInfo(): Pair<Long, Long> {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val total = totalBlocks * blockSize
        val available = availableBlocks * blockSize

        return Pair(total, available)
    }

    fun getExternalStorageInfo(): Pair<Long, Long>? {
        val externalStorage = Environment.getExternalStorageDirectory()
        return if (Environment.getExternalStorageState(externalStorage) == Environment.MEDIA_MOUNTED) {
            val stat = StatFs(externalStorage.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            Pair(totalBlocks * blockSize, availableBlocks * blockSize)
        } else null
    }

    fun formatStorageSize(sizeBytes: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            sizeBytes >= gb -> "%.2f GB".format(sizeBytes.toFloat() / gb)
            sizeBytes >= mb -> "%.2f MB".format(sizeBytes.toFloat() / mb)
            sizeBytes >= kb -> "%.2f KB".format(sizeBytes.toFloat() / kb)
            else -> "$sizeBytes B"
        }
    }

    fun getDisplayStatus(context: Context): String {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displays = displayManager.displays
        return displays.joinToString("\n") { display ->
            "Display ${display.displayId}: ${display.name}, State: ${display.state}"
        }
    }

    fun isHdmiConnectedQbic(): Boolean {
        return try {
            val file = File("/sys/class/drm/card0-HDMI-A-1/status")
            if (file.exists()) {
                val status = file.readText().trim()
                Log.d("QbicHdmi", "HDMI DRM status: $status")
                status.equals("connected", ignoreCase = true)
            } else {
                Log.w("QbicHdmi", "HDMI status file does not exist")
                false
            }
        } catch (e: Exception) {
            Log.e("QbicHdmi", "Error reading HDMI status", e)
            false
        }
    }

    fun getOSInfo(): String {
        val versionName = Build.VERSION.RELEASE ?: "Unknown"
        val sdkInt = Build.VERSION.SDK_INT

        return "OS Version: Android $versionName (SDK $sdkInt)"
    }

    @SuppressLint("HardwareIds")
    fun getDeviceUUID(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        )

        return try {
            UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
        } catch (e: Exception) {
            UUID.randomUUID().toString() // fallback
        }
    }


     suspend fun getDeviceDetails(context: Context, activity: FragmentActivity?): String {
        val ipAddress = getIPAddress()
        val cpuTemp = getCpuTemp()
        val getRamSize = getRamSize(context)
        val getScreenResolution = getScreenResolution(activity)
        val getDeviceSerial = getBuildSerial(context)
        val (total, available) = getInternalStorageInfo()
        val osInfoText = getOSInfo()
        val getDisplayStatus = getDisplayStatus(context)
        val getNetworkStatus = getNetworkStatus(context)
        val isHdmiConnected = isHdmiConnectedQbic()
        val getDeviceUUID = getDeviceUUID(context)
        val location = getLocationFromIpWho()

        return """
        Status: $getNetworkStatus
        IP Address: $ipAddress
        CPU Temp: $cpuTemp
        RAM Size: $getRamSize
        Screen Resolution: $getScreenResolution
        Device Serial: $getDeviceSerial
        Storage: Total = ${formatStorageSize(total)}, Available = ${formatStorageSize(available)}
        $osInfoText
        UUID: $getDeviceUUID
        Location: $location
        Display Status: $getDisplayStatus
        HDMI Status: $isHdmiConnected
    """.trimIndent()
    }

    fun saveToCache(context: Context, fileName: String, data: ByteArray): File? {
        return try {
            val file = File(context.cacheDir, fileName)
            val fos = FileOutputStream(file)
            fos.write(data)
            fos.close()
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }


    @SuppressLint("MissingPermission")
    fun startNetworkLocationUpdates(context: Context) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Log.e("QbicLocation", "NETWORK_PROVIDER is disabled")
            return
        }

        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            10_000L, // every 10 sec
            0f,
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val lat = location.latitude
                    val lon = location.longitude
                    Log.d("QbicLocation", "Lat: $lat, Lon: $lon")
                    // save or display location
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
        )
    }


    private suspend fun getLocationFromIpWho(): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://ipwho.is/")
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val inputStream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = inputStream.bufferedReader().use { it.readText() }
            Log.d("IPWHO", "Raw response: $response")

            if (!response.trim().startsWith("{")) return@withContext "Location unavailable"

            val json = JSONObject(response)
            if (!json.optBoolean("success")) return@withContext "Location unavailable"

            val city = json.optString("city")
            val region = json.optString("region")
            val country = json.optString("country")
            val latitude = json.optDouble("latitude")
            val longitude = json.optDouble("longitude")
          /*  City: $city
            Region: $region
            Country: $country
            Latitude: $latitude
            Longitude: $longitude*/

            """ $city, $region, $country """.trimIndent()

        } catch (e: Exception) {
            Log.e("IPWHO", "Exception: ${e.message}", e)
            "Location unavailable"
        }
    }
}

