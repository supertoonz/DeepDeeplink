package com.supertoonz.deferreddeeplink

import platform.Foundation.NSUserDefaults
import platform.UIKit.UIDevice

actual interface PlatformHelper {
    actual fun getDeviceFingerprint(): String
    actual fun hasCheckedBefore(): Boolean
    actual fun markAsChecked()
}

class IosPlatformHelper : PlatformHelper {
    
    private val userDefaults = NSUserDefaults.standardUserDefaults

    override fun getDeviceFingerprint(): String {
        val device = UIDevice.currentDevice
        return "\${device.name}_\${device.systemName}_\${device.systemVersion}"
    }

    override fun hasCheckedBefore(): Boolean {
        return userDefaults.boolForKey("has_checked_deferred")
    }

    override fun markAsChecked() {
        userDefaults.setBool(true, "has_checked_deferred")
    }
}
