package com.supertoonz.deferreddeeplink

import kotlinx.serialization.Serializable

@Serializable
data class DeferredInstallParameters(
    val utmSource: String? = null,
    val utmMedium: String? = null,
    val utmCampaign: String? = null
)

/**
 * Interface that platforms (Android, iOS) must implement to provide
 * their native fingerprinting and caching mechanisms.
 */
expect interface PlatformHelper {
    /**
     * E.g., Samsung Galaxy S24, iPhone 15 Pro
     */
    fun getDeviceFingerprint(): String
    
    /**
     * Has the app checked the backend before?
     */
    fun hasCheckedBefore(): Boolean
    
    /**
     * Mark that we've successfully pinged the backend
     */
    fun markAsChecked()

    /**
     * Attempts to fetch deterministic parameters directly from the OS.
     * Android: Google Play Install Referrer
     * iOS: UIPasteboard (Legacy approach)
     */
    suspend fun fetchDeterministicParams(): DeferredInstallParameters?
}
