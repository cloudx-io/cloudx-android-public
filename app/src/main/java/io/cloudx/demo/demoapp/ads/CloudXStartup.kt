package io.cloudx.demo.demoapp.ads

import android.content.Context
import io.cloudx.demo.demoapp.DemoConfig
import io.cloudx.demo.demoapp.DemoLog
import io.cloudx.sdk.CloudX
import io.cloudx.sdk.CloudXError
import io.cloudx.sdk.CloudXInitializationConfiguration
import io.cloudx.sdk.CloudXInitializationListener
import io.cloudx.sdk.CloudXLogLevel
import io.cloudx.sdk.CloudXSdkConfiguration

/**
 * Starts the CloudX SDK. Call [initialize] once, as early as possible (this app calls it from
 * `Application.onCreate`). To check later whether it succeeded, ask the SDK with
 * `CloudX.isInitialized()`.
 *
 * Copy as-is, with your own app key in place of [DemoConfig.APP_KEY] and your own logging in place
 * of [DemoLog].
 */
object CloudXStartup {

    private const val TAG = "CloudXStartup"

    fun initialize(context: Context) {
        // Verbose SDK logs help while integrating. Lower or remove this for release builds.
        CloudX.setMinLogLevel(CloudXLogLevel.VERBOSE)

        CloudX.initialize(
            context = context,
            configuration = CloudXInitializationConfiguration.builder(DemoConfig.APP_KEY).build(),
            listener = object : CloudXInitializationListener {
                override fun onInitialized(configuration: CloudXSdkConfiguration) {
                    DemoLog.i(TAG, "CloudX SDK initialized")
                }

                override fun onInitializationFailed(cloudXError: CloudXError) {
                    DemoLog.e(TAG, "CloudX SDK initialization failed: ${cloudXError.formattedMessage}")
                }
            },
        )
    }
}
