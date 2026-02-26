package com.supertoonz.deferreddeeplink.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.supertoonz.deferreddeeplink.AndroidPlatformHelper
import com.supertoonz.deferreddeeplink.DeferredDeeplinkSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var resultTextView: TextView
    private lateinit var sdk: DeferredDeeplinkSdk

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultTextView = findViewById(R.id.resultTextView)
        
        // Initialize the Core KMP SDK with the Android Platform actual implementations
        sdk = DeferredDeeplinkSdk(AndroidPlatformHelper(this))

        // Trigger the check asynchronously, since it involves network and disk reads
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // The SDK will first check Install Referrer, and if null, will ping the API
                val deferredParams = sdk.fetchDeferredParams()

                withContext(Dispatchers.Main) {
                    if (deferredParams != null) {
                        resultTextView.text = """
                            Deferred Deeplink Found!
                            
                            Source: ${deferredParams.utmSource}
                            Medium: ${deferredParams.utmMedium}
                            Campaign: ${deferredParams.utmCampaign}
                        """.trimIndent()
                    } else {
                        resultTextView.text = "No Deferred Deeplink Found or Already Processed."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resultTextView.text = "Error checking deferred deeplinks: ${e.message}"
                }
            }
        }
    }
}
