package com.supertoonz.deferreddeeplink

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual interface PlatformHelper {
    actual fun getDeviceFingerprint(): String
    actual fun hasCheckedBefore(): Boolean
    actual fun markAsChecked()
    actual suspend fun fetchDeterministicParams(): DeferredInstallParameters?
}

class AndroidPlatformHelper(private val context: Context) : PlatformHelper {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "deferred_deeplink_prefs", 
        Context.MODE_PRIVATE
    )

    override fun getDeviceFingerprint(): String {
        return "${Build.BRAND}_${Build.MODEL}_${Build.VERSION.SDK_INT}"
    }

    override fun hasCheckedBefore(): Boolean {
        return prefs.getBoolean("has_checked", false)
    }

    override fun markAsChecked() {
        prefs.edit().putBoolean("has_checked", true).apply()
    }

    override suspend fun fetchDeterministicParams(): DeferredInstallParameters? = suspendCoroutine { continuation ->
        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    try {
                        val response = referrerClient.installReferrer
                        val rawReferrer = response.installReferrer
                        
                        // Parse "utm_source=FB&utm_campaign=Spring"
                        val paramsMap = rawReferrer.split("&")
                            .mapNotNull { 
                                val parts = it.split("=")
                                if (parts.size == 2) parts[0] to parts[1] else null
                            }.toMap()
                            
                        val result = DeferredInstallParameters(
                            utmSource = paramsMap["utm_source"],
                            utmMedium = paramsMap["utm_medium"],
                            utmCampaign = paramsMap["utm_campaign"]
                        )
                        
                        referrerClient.endConnection()
                        continuation.resume(result)
                        return
                    } catch (e: Exception) {
                        referrerClient.endConnection()
                    }
                }
                continuation.resume(null)
            }

            override fun onInstallReferrerServiceDisconnected() {
                continuation.resume(null)
            }
        })
    }
}
