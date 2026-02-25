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
    private val apiEndpoint: String,
    private val platformHelper: PlatformHelper
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
