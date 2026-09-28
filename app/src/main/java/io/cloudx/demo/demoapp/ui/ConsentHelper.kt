package io.cloudx.demo.demoapp.ui

import android.app.Activity
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import io.cloudx.demo.demoapp.DemoLog

object ConsentHelper {

    private const val TAG = "ConsentHelper"

    fun requestConsentInformation(
        activity: Activity,
        debugGeography: Int? = null,
        testDeviceHashedId: String? = null,
        onConsentGathered: (canRequestAds: Boolean) -> Unit
    ) {
        val params = ConsentRequestParameters.Builder()
            .apply {
                if (debugGeography != null || testDeviceHashedId != null) {
                    val debugSettings = ConsentDebugSettings.Builder(activity)
                        .apply {
                            debugGeography?.let { setDebugGeography(it) }
                            testDeviceHashedId?.let { addTestDeviceHashedId(it) }
                        }
                        .build()
                    setConsentDebugSettings(debugSettings)
                }
            }
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        DemoLog.i(TAG, "Requesting consent information update...")

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                DemoLog.i(TAG, "Consent information update successful")
                DemoLog.i(
                    TAG,
                    "Consent status: ${getConsentStatusString(consentInformation.consentStatus)}"
                )
                DemoLog.i(
                    TAG,
                    "Is consent form available: ${consentInformation.isConsentFormAvailable}"
                )

                if (consentInformation.isConsentFormAvailable) {
                    loadAndShowConsentFormIfRequired(
                        activity,
                        consentInformation,
                        onConsentGathered
                    )
                } else {
                    DemoLog.i(TAG, "No consent form available, proceeding with ads")
                    onConsentGathered(canRequestAds(consentInformation))
                }
            },
            { formError ->
                DemoLog.e(TAG, "Consent information update failed: ${formError.message}")
                onConsentGathered(canRequestAds(consentInformation))
            }
        )
    }

    private fun loadAndShowConsentFormIfRequired(
        activity: Activity,
        consentInformation: ConsentInformation,
        onConsentGathered: (canRequestAds: Boolean) -> Unit
    ) {
        UserMessagingPlatform.loadConsentForm(
            activity,
            { consentForm ->
                DemoLog.i(TAG, "Consent form loaded successfully")

                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    DemoLog.i(TAG, "Showing consent form...")
                    consentForm.show(activity) { formError ->
                        if (formError != null) {
                            // A form that fails to show stays available, so retrying here would
                            // loop. Report the consent state as it is instead.
                            DemoLog.e(TAG, "Consent form show error: ${formError.message}")
                            onConsentGathered(canRequestAds(consentInformation))
                            return@show
                        }

                        DemoLog.i(TAG, "Consent form dismissed")
                        DemoLog.i(
                            TAG,
                            "New consent status: ${getConsentStatusString(consentInformation.consentStatus)}"
                        )

                        if (consentInformation.isConsentFormAvailable) {
                            loadAndShowConsentFormIfRequired(
                                activity,
                                consentInformation,
                                onConsentGathered
                            )
                        } else {
                            onConsentGathered(canRequestAds(consentInformation))
                        }
                    }
                } else {
                    DemoLog.i(TAG, "Consent already obtained, no need to show form")
                    onConsentGathered(canRequestAds(consentInformation))
                }
            },
            { formError ->
                DemoLog.e(TAG, "Consent form load failed: ${formError.message}")
                onConsentGathered(canRequestAds(consentInformation))
            }
        )
    }

    private fun canRequestAds(consentInformation: ConsentInformation): Boolean {
        return consentInformation.canRequestAds()
    }

    private fun getConsentStatusString(status: Int): String {
        return when (status) {
            ConsentInformation.ConsentStatus.UNKNOWN -> "UNKNOWN"
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> "NOT_REQUIRED"
            ConsentInformation.ConsentStatus.REQUIRED -> "REQUIRED"
            ConsentInformation.ConsentStatus.OBTAINED -> "OBTAINED"
            else -> "UNRECOGNIZED ($status)"
        }
    }
}
