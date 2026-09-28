# cloudx-android-public

A sample Android app that integrates the CloudX SDK and its network adapters from their published artifacts.

It shows Banner, MREC, Interstitial and Rewarded ads on the bottom tabs, and App Open, Native and the Mediation Debugger under More.

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
