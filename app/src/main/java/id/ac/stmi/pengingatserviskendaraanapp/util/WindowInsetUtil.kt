package id.ac.stmi.pengingatserviskendaraanapp.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object WindowInsetUtil {
    fun applySystemBarInsets(rootView: View) {
        val initialLeft = rootView.paddingLeft
        val initialTop = rootView.paddingTop
        val initialRight = rootView.paddingRight
        val initialBottom = rootView.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val sysBarsAndIme = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(
                initialLeft + sysBarsAndIme.left,
                initialTop + sysBarsAndIme.top,
                initialRight + sysBarsAndIme.right,
                initialBottom + sysBarsAndIme.bottom
            )
            insets
        }
    }
}
