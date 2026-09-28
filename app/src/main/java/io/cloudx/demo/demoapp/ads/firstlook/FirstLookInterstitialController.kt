package io.cloudx.demo.demoapp.ads.firstlook

/** The network that supplied an interstitial in this First Look pass. */
enum class FirstLookSource { CLOUDX, ADMOB }

/** Events from either interstitial SDK, with the source attached. */
sealed interface FirstLookEvent {
    data class Loaded(val source: FirstLookSource) : FirstLookEvent
    data class LoadFailed(val source: FirstLookSource, val message: String) : FirstLookEvent
    data class Shown(val source: FirstLookSource) : FirstLookEvent
    data class ShowFailed(val source: FirstLookSource, val message: String) : FirstLookEvent
    data class Closed(val source: FirstLookSource) : FirstLookEvent
    data class Clicked(val source: FirstLookSource) : FirstLookEvent
}

/** A one-use fullscreen ad source. The owning screen releases it on destruction. */
interface FirstLookInterstitialSource {
    var onEvent: ((FirstLookEvent) -> Unit)?
    val isReady: Boolean
    fun load()
    fun show()
    fun dispose()
}

/**
 * Gives CloudX the first load attempt. AdMob loads only after CloudX fails or is unavailable.
 * A new [load] after an ad closes starts a new CloudX-first pass.
 *
 * Pass null for [cloudX] when CloudX initialization failed, so every load goes straight to AdMob.
 * Call [onAdMobInitialized] once Google Mobile Ads finishes initializing; a fallback load requested
 * before that waits for it.
 */
class FirstLookInterstitialController(
    private val cloudX: FirstLookInterstitialSource?,
    private val adMob: FirstLookInterstitialSource,
    private val onEvent: (FirstLookEvent) -> Unit,
) {
    private var loading: FirstLookSource? = null
    private var showing: FirstLookSource? = null
    private var waitingForAdMob = false
    private var adMobInitialized = false
    private var disposed = false

    init {
        cloudX?.onEvent = ::handleEvent
        adMob.onEvent = ::handleEvent
    }

    val readySource: FirstLookSource?
        get() = when {
            disposed || showing != null -> null
            cloudX?.isReady == true -> FirstLookSource.CLOUDX
            adMob.isReady -> FirstLookSource.ADMOB
            else -> null
        }

    fun load() {
        if (disposed || showing != null || loading != null || waitingForAdMob || readySource != null) return

        if (cloudX == null) {
            loadAdMob()
        } else {
            loading = FirstLookSource.CLOUDX
            cloudX.load()
        }
    }

    fun onAdMobInitialized() {
        if (disposed) return
        adMobInitialized = true
        if (waitingForAdMob) {
            waitingForAdMob = false
            loadAdMob()
        }
    }

    /** Returns the selected source, or null when the app should continue without an ad. */
    fun show(): FirstLookSource? {
        val source = readySource ?: return null
        showing = source
        when (source) {
            FirstLookSource.CLOUDX -> cloudX?.show()
            FirstLookSource.ADMOB -> adMob.show()
        }
        return source
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        cloudX?.onEvent = null
        adMob.onEvent = null
        cloudX?.dispose()
        adMob.dispose()
    }

    private fun loadAdMob() {
        if (disposed || adMob.isReady) return
        if (!adMobInitialized) {
            waitingForAdMob = true
            return
        }
        loading = FirstLookSource.ADMOB
        adMob.load()
    }

    private fun handleEvent(event: FirstLookEvent) {
        if (disposed) return

        when (event) {
            is FirstLookEvent.Loaded -> {
                if (loading != event.source) return
                loading = null
                onEvent(event)
            }
            is FirstLookEvent.LoadFailed -> {
                if (loading != event.source) return
                loading = null
                if (event.source == FirstLookSource.CLOUDX) {
                    loadAdMob()
                } else {
                    onEvent(event)
                }
            }
            is FirstLookEvent.ShowFailed -> {
                if (showing != event.source) return
                if (event.source == FirstLookSource.CLOUDX && adMob.isReady) {
                    showing = FirstLookSource.ADMOB
                    adMob.show()
                } else {
                    showing = null
                    onEvent(event)
                }
            }
            is FirstLookEvent.Closed -> {
                if (showing != event.source) return
                showing = null
                onEvent(event)
            }
            is FirstLookEvent.Shown -> if (showing == event.source) onEvent(event)
            is FirstLookEvent.Clicked -> if (showing == event.source) onEvent(event)
        }
    }
}
