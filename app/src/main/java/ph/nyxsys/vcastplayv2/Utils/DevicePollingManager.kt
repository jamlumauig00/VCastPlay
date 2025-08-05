/**
 * Created by Jam on 2025-08-05.
 *
 */
package ph.nyxsys.vcastplayv2.Utils

import android.content.Context
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.*

class DevicePollingManager(
    private val context: Context,
    private val activityContext: FragmentActivity,
    private val targetView: TextView
) {
    private var pollingJob: Job? = null

    fun startPolling() {
        pollingJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val details = withContext(Dispatchers.IO) {
                    DeviceUtil.getDeviceDetails(context, activityContext)
                }
                withContext(Dispatchers.Main) {
                    targetView.text = details
                }
                delay(5000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
    }
}
