package ph.nyxsys.vcastplayv2.Helper

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionHelper(
    private val activity: Activity,
    private val requestCode: Int
) {

    interface PermissionCallback {
        fun onPermissionGranted()
        fun onPermissionDenied(deniedPermissions: List<String>)
    }

    private var permissionCallback: PermissionCallback? = null

    fun requestPermissions(
        permissions: Array<String>,
        callback: PermissionCallback
    ) {
        permissionCallback = callback

        val deniedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (deniedPermissions.isEmpty()) {
            callback.onPermissionGranted()
        } else {
            ActivityCompat.requestPermissions(activity, deniedPermissions.toTypedArray(), requestCode)
        }
    }

    fun handlePermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (this.requestCode != requestCode) return

        val deniedPermissions = permissions.indices
            .filter { grantResults[it] != PackageManager.PERMISSION_GRANTED }
            .map { permissions[it] }

        permissionCallback?.let {
            if (deniedPermissions.isEmpty()) {
                it.onPermissionGranted()
            } else {
                it.onPermissionDenied(deniedPermissions)
            }
        }

        permissionCallback = null
    }
}
