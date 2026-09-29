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
 * Starts the CloudX SDK. Each demo flow calls [initialize] when its screen opens. The SDK's automatic
 * startup warm-ups run separately at process start.
 *
 * Sets the user's privacy flags before calling [CloudX.initialize], since CloudX reads them at
 * initialization. To check later whether it succeeded, call [CloudX.isInitialized].
 *
 * Calling [initialize] again is safe: the SDK does not start a second initialization. It reports
 * the result of the current one, and starts a new attempt only if the last one failed.
 *
 * Copy as-is, with your own app key in place of [DemoConfig.APP_KEY] and your own logging in place
 * of [DemoLog].
 */
object CloudXInit {

    private const val TAG = "CloudXInit"

    /** [listener] gets the result on the main thread; pass null when the caller does not need it. */
    fun initialize(context: Context, listener: CloudXInitializationListener?) {
        // Verbose SDK logs help while integrating. Lower or remove this for release builds.
        CloudX.setMinLogLevel(CloudXLogLevel.VERBOSE)

        // Without a consent management platform, pass the user's privacy choices before initializing.
        // Replace these values with the choices your own consent flow collected.
        CloudX.setHasUserConsent(true)
        CloudX.setDoNotSell(false)

        CloudX.initialize(
            context = context,
            configuration = CloudXInitializationConfiguration.builder(DemoConfig.APP_KEY).build(),
            listener = object : CloudXInitializationListener {
                override fun onInitialized(configuration: CloudXSdkConfiguration) {
                    DemoLog.i(TAG, "CloudX SDK initialized")
                    listener?.onInitialized(configuration)
                }

                override fun onInitializationFailed(cloudXError: CloudXError) {
                    DemoLog.e(TAG, "CloudX SDK initialization failed: ${cloudXError.formattedMessage}")
                    listener?.onInitializationFailed(cloudXError)
                }
            },
        )
    }
}
