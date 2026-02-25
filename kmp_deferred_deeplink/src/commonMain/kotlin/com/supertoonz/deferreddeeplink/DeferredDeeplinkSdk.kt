package com.supertoonz.deferreddeeplink

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class InstallRequest(val device_id: String)

@Serializable
private data class InstallResponse(
    val success: Boolean,
    val data: DeferredInstallParameters? = null
)

class DeferredDeeplinkSdk(
    private val platformHelper: PlatformHelper,
    private val apiEndpoint: String = "https://deferred-deeplink-api-327503979782.us-central1.run.app/api/install"
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    /**
     * Retrieves deferred parameters from the backend on first launch.
     * Returns null if already checked or if there's an error.
     */
    suspend fun fetchDeferredParams(): DeferredInstallParameters? {
        try {
            if (platformHelper.hasCheckedBefore()) {
                return null
            }

            // 1. Try to fetch 100% accurate Deterministic parameters FIRST from the OS
            val deterministicParams = platformHelper.fetchDeterministicParams()
            if (deterministicParams != null && 
                (deterministicParams.utmSource != null || deterministicParams.utmCampaign != null)) {
                
                println("[DeferredDeeplinkSdk] Deterministic match found!")
                platformHelper.markAsChecked()
                return deterministicParams
            }

            // 2. Fallback to probabilistic Fingerprint Backend Match
            println("[DeferredDeeplinkSdk] No deterministic params, falling back to Fingerprint")
            val fingerprint = platformHelper.getDeviceFingerprint()

            val response: InstallResponse = client.post(apiEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(InstallRequest(device_id = fingerprint))
            }.body()

            // Mark as checked to prevent duplicate pings
            platformHelper.markAsChecked()

            if (response.success) {
                return response.data
            }
            
        } catch (e: Exception) {
            println("[DeferredDeeplinkSdk] Network Error: \${e.message}")
        }
        
        return null
    }
}
