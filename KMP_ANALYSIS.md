# Kotlin Multiplatform (KMP) for Deferred Deep Linking

This document analyzes the architecture and feasibility of extracting the Deferred Deep Linking logic into a reusable **Kotlin Multiplatform (KMP)** library. This library would hold the core business logic (HTTPS networking, fingerprint generation, local storage) and be consumed natively by iOS (Swift) and Android (Kotlin) apps, or wrapped by a Flutter plugin.

## 1. Why Kotlin Multiplatform?

KMP is an excellent choice for building internal libraries ("SDKs") where you want to share business logic but maintain 100% native UI and performance.

*   **Write Once, Run Anywhere:** Network calls, data parsing (JSON), and caching logic are written once in Kotlin.
*   **Native Output:** 
    *   For Android, it outputs an `.aar` (standard Android library).
    *   For iOS, it compiles directly to a native `.xcframework` using Kotlin/Native, meaning iOS developers consume it exactly like a Swift framework. No embedded virtual machines (unlike Flutter or React Native).
*   **Ideal for 'Invisible' Logic:** Since Deferred Deep Linking has zero UI components, it is the perfect use case for KMP.

## 2. KMP Architecture for Deferred Deep Linking

A KMP library is typically divided into three source sets: `commonMain`, `androidMain`, and `iosMain`. 

### A. The Core Logic (`commonMain`)
This is the shared Kotlin code that runs identically on both platforms. It handles the orchestration.

*   **HttpClient (Ktor):** Makes the `POST /api/install` HTTP call.
*   **Data Models (Kotlinx Serialization):** Defines the `DeferredInstallParameters` (UTM params) and parses the JSON response.
*   **Orchestrator class:** 
    ```kotlin
    // commonMain/src/DeferredDeeplinkSdk.kt
    class DeferredDeeplinkSdk(private val platformHelper: PlatformHelper) {
        
        suspend fun fetchDeferredParams(): DeferredInstallParameters? {
            // 1. Check if we've already done this
            if (platformHelper.hasCheckedBefore()) return null
            
            // 2. Get device fingerprint
            val fingerprint = platformHelper.getDeviceFingerprint()

            // 3. Make HTTP call
            val response = makeNetworkRequest(fingerprint)
            
            // 4. Save state
            platformHelper.markAsChecked()
            
            return response
        }
    }
    ```

### B. Platform-Specific Logic (`androidMain` & `iosMain`)
Whenever the `commonMain` logic needs to do something specific to the OS, we use the `expect` / `actual` keyword pattern. 

**Expect Definition (`commonMain`):**
```kotlin
expect interface PlatformHelper {
    fun getDeviceFingerprint(): String
    fun hasCheckedBefore(): Boolean
    fun markAsChecked()
}
```

**Android Implementation (`androidMain`):**
*   Uses `android.os.Build` to get device models (e.g., Samsung Galaxy S24).
*   Uses `SharedPreferences` or `DataStore` to save the boolean flag.
*   Can also uniquely tap into the **Google Play Install Referrer API** here, drastically improving Android accuracy!

**iOS Implementation (`iosMain`):**
*   Uses `UIDevice.current` to get device models (e.g., iPhone 15 Pro).
*   Uses `NSUserDefaults` to save the boolean flag.

## 3. How Consumers Use It

If you publish this as an SDK, this is how application developers experience it:

**iOS Developer (Swift):**
```swift
import DeferredDeeplinkSdk

let sdk = DeferredDeeplinkSdk(helper: IosPlatformHelper())
Task {
    if let params = await sdk.fetchDeferredParams() {
        print("Got UTM Source: \(params.utmSource)")
    }
}
```

**Android Developer (Kotlin):**
```kotlin
import com.supertoonz.deferreddeeplink.DeferredDeeplinkSdk

val sdk = DeferredDeeplinkSdk(AndroidPlatformHelper(context))
lifecycleScope.launch {
    val params = sdk.fetchDeferredParams()
    params?.let { Log.d("UTM", it.utmSource) }
}
```

## 4. Challenges & Drawbacks

1. **The Concurrency Model:** Kotlin Coroutines map cleanly to Kotlin (Android), but bridging them perfectly to Swift's `async/await` can sometimes require wrapper libraries like SKIE (Swift Kotlin Interface Enhancer).
2. **Ecosystem Build Setup:** Configuring Gradle to spit out both an `.aar` and an `.xcframework` can be complex initially.
3. **If You Only Use Flutter:** If your primary consumer is just your own Flutter apps (like `DeepDeeplink`), writing it in pure Dart (like you already did) is actually simpler. You only need KMP if you plan to share this logic with *other* teams who are building purely Native iOS and Native Android apps.

## Conclusion

Building this as a Kotlin Multiplatform library is **highly feasible and architecturally sound**. It is arguably the "right" way to build internal tech infrastructure (SDKs) today, especially for headless business logic mapping data to network requests.
