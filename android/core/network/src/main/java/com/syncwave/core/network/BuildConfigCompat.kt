package com.syncwave.core.network

object BuildConfigCompat {
    @Volatile private var overrideBaseUrl: String? = null
    fun setBaseUrl(url: String) { overrideBaseUrl = url }
    fun baseUrl(): String =
        overrideBaseUrl ?: "https://syncwave.vercel.app"
}
