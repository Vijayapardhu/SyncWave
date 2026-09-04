package com.syncwave.core.media

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager

/**
 * Wraps the system [MediaProjectionManager] so the rest of the app can request
 * capture without holding an Activity reference.
 */
class MediaProjectionRequester(private val context: Context) {

    private val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
        as MediaProjectionManager

    fun createIntent(): Intent = mgr.createScreenCaptureIntent()

    fun obtainProjection(resultCode: Int, data: Intent): MediaProjection =
        mgr.getMediaProjection(resultCode, data)
            ?: error("MediaProjection unavailable")

    /**
     * Same as [obtainProjection] but returns null on failure instead of
     * throwing. Useful for audio-only paths where the projection is a
     * transport for audio capture, not a hard requirement on the same call.
     */
    fun tryObtainProjection(resultCode: Int, data: Intent): MediaProjection? =
        mgr.getMediaProjection(resultCode, data)
}
