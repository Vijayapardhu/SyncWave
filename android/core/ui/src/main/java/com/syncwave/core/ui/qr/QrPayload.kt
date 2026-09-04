package com.syncwave.core.ui.qr

/**
 * The payload encoded into a SyncWave QR code. The scheme is
 * `syncwave://join/<code>` so the scanner knows it's ours and can
 * route directly to the join flow without an extra prompt.
 */
object QrPayload {
    private const val SCHEME = "syncwave"
    private const val HOST = "join"
    private const val PREFIX = "$SCHEME://$HOST/"

    fun forRoom(code: String): String = "$PREFIX${code.uppercase()}"

    fun extractCode(raw: String): String? {
        if (raw.isBlank()) return null
        val trimmed = raw.trim()
        if (!trimmed.startsWith(PREFIX, ignoreCase = true)) return null
        val code = trimmed.removePrefix(PREFIX).removePrefix("$PREFIX".lowercase()).trim()
        if (code.length != 6) return null
        return code.uppercase()
    }
}
