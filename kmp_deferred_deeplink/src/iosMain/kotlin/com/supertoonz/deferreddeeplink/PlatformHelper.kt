package com.supertoonz.deferreddeeplink

import platform.Foundation.NSUserDefaults
import platform.UIKit.UIDevice
import platform.UIKit.UIPasteboard

actual interface PlatformHelper {
    actual fun getDeviceFingerprint(): String
    actual fun hasCheckedBefore(): Boolean
    actual fun markAsChecked()
    actual suspend fun fetchDeterministicParams(): DeferredInstallParameters?
}

class IosPlatformHelper : PlatformHelper {
    
    private val userDefaults = NSUserDefaults.standardUserDefaults

    override fun getDeviceFingerprint(): String {
        val device = UIDevice.currentDevice
        return "${device.name}_${device.systemName}_${device.systemVersion}"
    }

    override fun hasCheckedBefore(): Boolean {
        return userDefaults.boolForKey("has_checked_deferred")
    }

    override fun markAsChecked() {
        userDefaults.setBool(true, "has_checked_deferred")
    }

    override suspend fun fetchDeterministicParams(): DeferredInstallParameters? {
        val clipboardString = UIPasteboard.generalPasteboard.string
        
        // Let's assume the web page copied something like "deeplink:utm_source=FB&utm_campaign=Spring"
        if (clipboardString != null && clipboardString.startsWith("deeplink:")) {
            val query = clipboardString.removePrefix("deeplink:")
            val paramsMap = query.split("&")
                .mapNotNull { 
                    val parts = it.split("=")
                    if (parts.size == 2) parts[0] to parts[1] else null
                }.toMap()
                
            return DeferredInstallParameters(
                utmSource = paramsMap["utm_source"],
                utmMedium = paramsMap["utm_medium"],
                utmCampaign = paramsMap["utm_campaign"]
            )
        }
        return null
    }
}
