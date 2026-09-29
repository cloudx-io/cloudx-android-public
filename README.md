# cloudx-android-public

A sample Android app that integrates the CloudX SDK and its network adapters from their published artifacts.

## App flow

The app opens on an Options screen that picks a demo flow:

```
Options  ──  General      ──>  the CloudX integration sample
         ├─  First Look   ──>  CloudX-first interstitial with AdMob fallback
         └─  Arbiter/TPA  ──>  CloudX and AdMob in parallel, Trusted Arbiter picks
```

General shows Banner, MREC, Interstitial and Rewarded ads on the bottom tabs, and App Open, Native and the Mediation Debugger under More. First Look loads a CloudX interstitial first. It loads the AdMob test interstitial only if CloudX cannot fill or initialize. Arbiter/TPA loads the CloudX and AdMob interstitials at the same time and lets Trusted Arbiter pick which one to show.

The Options screen closes once you pick a flow, so Back from any flow leaves the app. It shows again only when the app starts from scratch. It makes no SDK calls: each flow calls `CloudX.initialize` when its screen opens. The SDK's automatic startup warm-ups run separately at process start.

First Look prepares another CloudX-first pass after an interstitial closes. If both sources fail, it retries with increasing delays. The AdMob app ID and interstitial ID are Google's test IDs; replace them with your own IDs before using this flow in a production app.

The First Look flow lives in [`app/src/main/java/io/cloudx/demo/demoapp/ads/firstlook/`](app/src/main/java/io/cloudx/demo/demoapp/ads/firstlook/): `FirstLookInterstitialController.kt` and its two sources, `CloudXFirstLookSource.kt` and `AdMobFirstLookSource.kt`. Copy all three together. Both sources log through the demo's `DemoLog`; swap in your own logging when you copy them. The host screen, `ui/FirstLookActivity.kt`, initializes Google Mobile Ads, waits up to 15 seconds for the CloudX initialization result, retries a failed load or show with a 2 to 60 second backoff, and connects the controller to the Show button and status text.

Arbiter/TPA (Trusted Arbiter, third-party arbitration) is the other way to run CloudX next to AdMob. Both interstitials load in parallel. Once both have settled, loaded or failed, the loaded ones become bids and `CloudX.arbiter` returns the platform to show. The winner is stored, so Show displays it with no network call; if no winner is prepared, a real app carries on without an ad. After the ad closes, each platform without a fill reloads, one that still holds an ad keeps it, and a new round runs. The SDK owns the arbiter's timeout and fallback, so the demo neither times the call out nor compares prices: a single bid wins without a service call.

AdMob bids carry no price. CloudX prices them from the revenue the app reports after each AdMob impression, so every AdMob paid event goes to `CloudX.reportRevenueData`. That call is a required part of the integration. The status line shows what it returned; Google's test ad unit pays 0, which CloudX does not keep as a price. If CloudX does not initialize, AdMob is the only candidate and wins each round without an arbiter call. The flow covers the interstitial; rewarded follows the same controller with the rewarded calls, while banner, MREC and native arbitrate first and then render the winner, which this demo does not show. The pattern is documented in the [Trusted Arbiter guide](https://docs.cloudx.io/en/android/trusted-arbiter).

To integrate it, copy [`app/src/main/java/io/cloudx/demo/demoapp/ads/arbiter/ArbiterInterstitialController.kt`](app/src/main/java/io/cloudx/demo/demoapp/ads/arbiter/ArbiterInterstitialController.kt). It holds every load, show, arbiter and revenue call of the flow and logs through the demo's `DemoLog`; swap in your own logging when you copy it. The host screen, `ui/ArbiterActivity.kt`, initializes both SDKs the same way as First Look, retries with a 2 to 60 second backoff when neither platform fills or a show fails, and connects the controller to the Show button and status text.

Trusted Arbiter has to be enabled for your app in the CloudX dashboard. Until it is, the SDK decides a round with more than one bid locally: the highest comparable price wins, and an AdMob bid with no revenue history yet cannot win against CloudX.

## Samples

Each ad format is one self-contained class in [`app/src/main/java/io/cloudx/demo/demoapp/ads/`](app/src/main/java/io/cloudx/demo/demoapp/ads/), ready to copy into your app:

| File | Shows |
|---|---|
| `CloudXInit.kt` | SDK initialization and privacy flags |
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
