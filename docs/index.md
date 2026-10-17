# ChargeSound

**Charge sound and full-screen charging animations for Android.**

A utility app that plays a sound, vibrates, and shows a charging animation the moment you plug
in your phone — with 5 built-in sounds, 3 built-in neon themes, and 6 more unlockable with
rewarded ads. Bring your own MP3 or video if you prefer.

| | |
|---|---|
| Package | `com.chargesound.app` |
| Category | Utilities |
| Ad network | ironSource LevelPlay (banner · native · rewarded) |

AdMob is **not** used. OneStore is not on AdMob's supported-store list, so an AdMob-linked build
gets distribution-limited there. See [Ad network](#ad-network) below.

---

## Privacy

**→ [Privacy Policy (English)](privacy-policy.html)** &nbsp;·&nbsp;
**[개인정보처리방침 (한국어)](privacy-policy-ko.html)**

The app has no accounts, no backend, and no developer-side data collection. Your settings stay on
your device. The only data that leaves your device is the standard advertising identifier handled by
our ad partner (ironSource / AppLovin).

---

## Localization

| Locale | File |
|---|---|
| English (default) | `app/src/main/res/values/strings.xml` |
| Korean | `app/src/main/res/values-ko/strings.xml` |
| Spanish | `app/src/main/res/values-es/strings.xml` |

English is the default so that every non-Korean, non-Spanish device gets a fully translated UI.
Korean and Spanish devices pick up their strings automatically from the device locale.

Adding a locale: copy `values/strings.xml` to `values-<locale>/strings.xml` and translate. Keep the
placeholder order (`%1$s`, `%2$d`, …) **identical** to the default file — Android throws at build
time when a translated string renumbers its arguments.

`app_name`, `unit_celsius` and `unit_volts` are intentionally English-only: they are the store
listing name and SI unit symbols, which should not be localised.

---

## Ad network

ironSource LevelPlay via `com.unity3d.ads-mediation:mediation-sdk`. AdMob was evaluated and
rejected on purpose:

| Network | Why not |
|---|---|
| AdMob | OneStore is not a supported store for AdMob-linked publishers. The build is distribution-limited there. |
| AppLovin standalone | Account signup only accepts Play Store / App Store URLs — it rejects a OneStore URL. |

Consequence: **`AdUnits.APP_KEY` must be filled in** from
LevelPlay → `Account` → `My Account` → `Keys`. While it is empty, `AdManager.isConfigured` is
`false`, the SDK never initializes, and the app simply shows no ads — every other feature works.

## Ad policy

The app is built to stay inside the ad-network program policies:

- **No interstitial ads and no app-open ads.** Utility apps with a continuous, on/off-loop use case
  are steered toward banner, native, and rewarded formats instead. The charging animation activity
  calls `AdManager.setCoreFeatureBusy(true)` so nothing can interrupt playback.
- **Ads never block core features.** Declining or failing to load an ad always leaves the app fully
  usable. Every built-in sound and theme is free; premium content is additional, not a gate.
- **Rewards are stated up front.** The unlock rule ("watch 2 ads") and the progress toward it are
  shown on the item itself.
- **Permission denial degrades gracefully.** Without `SYSTEM_ALERT_WINDOW` the charging sound and
  vibration keep working and a notification is shown instead.

---

## Ad units

Real unit IDs live in `app/src/main/java/com/example/ad/AdUnits.kt`.

| Format | Placement ID |
|---|---|
| Banner | `9mcw3kw9sziwg1z` |
| Native (sponsored card) | `ucwxlw42iknaw9ra` |
| Rewarded | `25usihf6uxpszb0u` |

`check-ads.ps1` diagnoses a device that shows no ads (checks the LevelPlay SDK key, placement IDs,
mediation status, and the live logcat).

---

## Build

**Use a JDK whose `jlink` accepts the Android platform jar.** JDK 26 rejects it:

```
jlink build 'Oracle Corporation-26.0.2.1' cannot find the build signature in the
java.base specified on module path, likely from an earlier build.
```

Any full JDK 17–25 works. JDK 21 is the most battle-tested choice.

```powershell
# 1) JDK — must be a full JDK 17~25, NOT 26+, and NOT a JBR
$env:JAVA_HOME     = "C:\Program Files\Eclipse Adoptium\jdk-21"

# 2) SDK — or write sdk.dir into local.properties
$env:ANDROID_HOME  = "$env:LOCALAPPDATA\Android\Sdk"

# 3) Release signing
$env:KEYSTORE_PATH = "<path to my-upload-key.jks>"
$env:STORE_PASSWORD = "..."
$env:KEY_PASSWORD   = "..."
$env:KEY_ALIAS      = "upload"

# 4) Version — must be higher than whatever is already on the store
gradle :app:assembleRelease -PVERSION_CODE=3 -PVERSION_NAME=1.2.0
```

Output: `app/build/outputs/apk/release/app-release.apk`

> `versionCode` has to increase on **every** store submission — OneStore and Play reject a value that
> is not greater than the last one. The defaults live at the top of `app/build.gradle.kts`.

CI (GitHub Actions) builds both debug and release: `.github/workflows/build-apk.yml`.

---

## Content licensing

Every sound and animation in the app is **procedurally generated at runtime** — audio is synthesised
from PCM waveforms, animations are rendered frame by frame. No third-party media assets are bundled.

If you ever want to serve remote content, only fill in `PremiumSound.remoteUrl` /
`PremiumAnimation.remoteUrl` with files you created yourself or hold a commercial licence for.
Redistributing media downloaded from other platforms violates the store's intellectual property
policy and risks the app *and* the ad account.