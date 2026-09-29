# cloudx-android-public

A sample Android app that integrates the CloudX SDK and its network adapters from their published artifacts.

## App flow

The app opens on an Options screen that picks a demo flow:

```
Options  ──  General      ──>  the CloudX integration sample
         ├─  First Look   ──>  CloudX-first interstitial with AdMob fallback
         └─  Arbiter/TPA  (not available yet)
```

General shows Banner, MREC, Interstitial and Rewarded ads on the bottom tabs, and App Open, Native and the Mediation Debugger under More. First Look loads a CloudX interstitial first. It loads the AdMob test interstitial only if CloudX cannot fill or initialize. Arbiter/TPA remains disabled.

The Options screen closes once you pick a flow, so Back from General or First Look leaves the app. It shows again only when the app starts from scratch. It makes no SDK calls: each flow initializes CloudX when its screen opens, so nothing SDK-related runs until you pick one.

First Look prepares another CloudX-first pass after an interstitial closes. If both sources fail, it retries with increasing delays. The AdMob app ID and interstitial ID are Google's test IDs; replace them with your own IDs before using this flow in a production app.

The First Look flow lives in [`app/src/main/java/io/cloudx/demo/demoapp/ads/firstlook/`](app/src/main/java/io/cloudx/demo/demoapp/ads/firstlook/): `FirstLookInterstitialController.kt` and its two sources, `CloudXFirstLookSource.kt` and `AdMobFirstLookSource.kt`. Copy all three together. Both sources log through the demo's `DemoLog`; swap in your own logging when you copy them. The host screen, `ui/FirstLookActivity.kt`, initializes Google Mobile Ads, waits up to 15 seconds for the CloudX initialization result, retries a failed load or show with a 2 to 60 second backoff, and connects the controller to the Show button and status text.

## Samples

Each ad format is one self-contained class in [`app/src/main/java/io/cloudx/demo/demoapp/ads/`](app/src/main/java/io/cloudx/demo/demoapp/ads/), ready to copy into your app:

| File | Shows |
|---|---|
| `CloudXStartup.kt` | SDK initialization and privacy flags |
| `BannerAd.kt` | 320x50 banner |
| `MrecAd.kt` | 300x250 MREC |
| `InterstitialAd.kt` | Interstitial |
| `RewardedAd.kt` | Rewarded, with the reward callback |
| `AppOpenAd.kt` | App Open |
| `NativeAd.kt` | Native, both loading flows (needs `res/layout/native_ad_layout.xml`) |

Each class logs through the demo's `DemoLog`; swap in your own logging when you copy it. Everything under `ui/` is this app's screens and only wires buttons to those classes.

## Build and run

```sh
./gradlew :app:installDebug
```

The SDK and adapter set follows the [Android integration guide](https://docs.cloudx.io/en/android/integration).
