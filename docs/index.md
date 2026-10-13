# ChargeSound

**Charge sound and full-screen charging animations for Android.**

A utility app that plays a sound, vibrates, and shows a charging animation the moment you plug
in your phone — with 5 built-in sounds, 3 built-in neon themes, and 6 more unlockable with
rewarded ads. Bring your own MP3 or video if you prefer.

| | |
|---|---|
| Package | `com.chargesound.app` |
| Category | Utilities |
| AdMob | Banner · Native · Rewarded · App open |

---

## Privacy

**→ [Privacy Policy (English)](privacy-policy.html)** &nbsp;·&nbsp;
**[개인정보처리방침 (한국어)](privacy-policy-ko.html)**

The app has no accounts, no backend, and no developer-side data collection. Your settings stay on
your device. The only data that leaves your device is the standard advertising identifier handled by
Google AdMob.

---

## Localization

| Locale | File |
|---|---|
| English (default) | `app/src/main/res/values/strings.xml` |
| Korean | `app/src/main/res/values-ko/strings.xml` |

English is the default so that every non-Korean device gets a fully translated UI. Korean devices
pick up the Korean strings automatically from the device locale.

Adding a locale: copy `values/strings.xml` to `values-<locale>/strings.xml` and translate. Keep the
placeholder order (`%1$s`, `%2$d`, …) identical to the default file.

---

## Ad policy

The app is built to stay inside the AdMob program policies:

- **No interstitial ads.** Utility apps with a continuous, on/off-loop use case are steered toward
  banner, native, and rewarded formats instead.
- **Ads never block core features.** Declining or failing to load an ad always leaves the app fully
  usable. Every built-in sound and theme is free; premium content is additional, not a gate.
- **Rewards are stated up front.** The unlock rule ("watch 2 ads") and the progress toward it are
  shown on the item itself.
- **Permission denial degrades gracefully.** Without `SYSTEM_ALERT_WINDOW` the charging sound and
  vibration keep working and a notification is shown instead.
- **Ads are suppressed during core features.** The charging animation and sound preview block app-open
  ads from interrupting playback.

---

## Ad units

Real unit IDs live in `app/src/main/java/com/example/ad/AdUnits.kt`.

Test IDs are kept in the `debug` source set only, so they can never be compiled into a release APK:

```
app/src/debug/.../AdUnitIds.kt    ← Google test IDs
app/src/release/.../AdUnitIds.kt  ← production IDs
app/src/main/.../AdUnits.kt       ← app ID + policy constants
```

Verify a release build before shipping:

```powershell
# release APK must contain no 3940256099942544 (test publisher) IDs
```

---

## Build

```powershell
$env:JAVA_HOME      = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME   = "$env:LOCALAPPDATA\Android\Sdk"
$env:KEYSTORE_PATH  = "<path to my-upload-key.jks>"
$env:STORE_PASSWORD = "..."
$env:KEY_PASSWORD   = "..."
$env:KEY_ALIAS      = "upload"

gradle :app:assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

CI (GitHub Actions) builds both debug and release: `.github/workflows/build-apk.yml`.

---

## Content licensing

Every sound and animation in the app is **procedurally generated at runtime** — audio is synthesised
from PCM waveforms, animations are rendered frame by frame. No third-party media assets are bundled.

If you ever want to serve remote content, only fill in `PremiumSound.remoteUrl` /
`PremiumAnimation.remoteUrl` with files you created yourself or hold a commercial licence for.
Redistributing media downloaded from other platforms violates Play's intellectual property policy
and risks the app *and* the AdMob account.