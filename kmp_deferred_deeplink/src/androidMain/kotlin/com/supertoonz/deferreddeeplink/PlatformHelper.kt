package com.supertoonz.deferreddeeplink

import android.content.Context
import android.content.SharedPreferences
import android.os.Build

actual interface PlatformHelper {
    actual fun getDeviceFingerprint(): String
    actual fun hasCheckedBefore(): Boolean
    actual fun markAsChecked()
}

class AndroidPlatformHelper(private val context: Context) : PlatformHelper {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "deferred_deeplink_prefs", 
        Context.MODE_PRIVATE
    )

    override fun getDeviceFingerprint(): String {
        return "\${Build.BRAND}_\${Build.MODEL}_\${Build.VERSION.SDK_INT}"
    }

    override fun hasCheckedBefore(): Boolean {
        return prefs.getBoolean("has_checked", false)
    }

    override fun markAsChecked() {
        prefs.edit().putBoolean("has_checked", true).apply()
    }
}
