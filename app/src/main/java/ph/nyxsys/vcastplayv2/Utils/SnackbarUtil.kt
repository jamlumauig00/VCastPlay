/**
 * Created by Jam on 2025-08-05.
 *
 */
package ph.nyxsys.vcastplayv2.Utils

import android.app.Activity
import android.graphics.Color
import android.view.*
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.graphics.drawable.toDrawable
import ph.nyxsys.vcastplayv2.databinding.LayoutCustomSnackbarBinding

object SnackbarUtil {
    private var popup: PopupWindow? = null

    fun showExitSnackbar(activity: Activity, onDismiss: () -> Unit) {
        if (popup?.isShowing == true) return

        val viewBinding = LayoutCustomSnackbarBinding.inflate(LayoutInflater.from(activity))
        popup = PopupWindow(
            viewBinding.root,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isClippingEnabled = true
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            animationStyle = android.R.style.Animation_Toast

            viewBinding.snackbarAction.setOnClickListener {
                dismiss()
                onDismiss()
            }

            showAtLocation(activity.findViewById(android.R.id.content), Gravity.BOTTOM, 0, 0)

            viewBinding.root.postDelayed({ if (isShowing) dismiss() }, 3000)
        }
    }
}
