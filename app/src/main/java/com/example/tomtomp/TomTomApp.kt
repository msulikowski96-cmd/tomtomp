package com.example.tomtomp

import android.app.Application
import com.tomtom.sdk.common.configuration.buildSdkConfiguration
import com.tomtom.sdk.init.TomTomSdk
import com.tomtom.sdk.telemetry.UserConsent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TomTomApp : Application() {

    private val _sdkReady = MutableStateFlow<SdkState>(SdkState.Initializing)
    val sdkState: StateFlow<SdkState> = _sdkReady

    override fun onCreate() {
        super.onCreate()
        // The SDK must be initialized exactly once and off the main thread.
        CoroutineScope(Dispatchers.Default).launch {
            _sdkReady.value = runCatching {
                if (!TomTomSdk.isInitialized) {
                    val configuration = buildSdkConfiguration(
                        context = applicationContext,
                        apiKey = BuildConfig.TOMTOM_API_KEY,
                        // Replace with a real consent prompt before shipping.
                        telemetryUserConsent = { UserConsent.TelemetryOff },
                    )
                    TomTomSdk.initialize(applicationContext, configuration)
                }
                SdkState.Ready
            }.getOrElse { SdkState.Failed(it.message ?: it.toString()) }
        }
    }
}

sealed interface SdkState {
    data object Initializing : SdkState
    data object Ready : SdkState
    data class Failed(val message: String) : SdkState
}
