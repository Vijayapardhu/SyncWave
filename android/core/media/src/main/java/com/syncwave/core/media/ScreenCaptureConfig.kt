package com.syncwave.core.media

import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager
import android.content.Context

data class ScreenCaptureConfig(
    val width: Int,
    val height: Int,
    val densityDpi: Int,
    val fps: Int = 30
)

object ScreenCaptureConfigFactory {
    fun fromContext(context: Context, fps: Int = 30): ScreenCaptureConfig {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val (w, h) = when (val rotation = wm.defaultDisplay.rotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> metrics.widthPixels to metrics.heightPixels
            else -> metrics.heightPixels to metrics.widthPixels
        }
        return ScreenCaptureConfig(w, h, metrics.densityDpi, fps)
    }
}
