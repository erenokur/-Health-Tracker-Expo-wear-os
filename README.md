# Health Tracker — Wear OS Companion App

Standalone Kotlin + Jetpack Compose (Wear Compose) app. Installed directly
to your watch via `adb install` — it is **not** bundled inside the phone
app's APK, so it doesn't need to share a package name or signing
certificate with it. It only needs Google Play Services (Wearable) present
on the watch, which is standard on any real Wear OS 3+ device including
Wear OS 6.

## What it does

- **Tansiyon Ekle**: 3 fields (Büyük/Sys, Küçük/Dia, Nabız) + KAYDET button.
  Saves with the current time and sends it to the paired phone.
- **İlaç İçildi**: tap to speak/type a medication name (opens the system
  voice/keyboard input — there's no synced medication list on the watch yet,
  see "Known limitation" below), toggle Aç/Tok, KAYDET button.

Both screens send a JSON message to every currently-connected phone via the
**Wearable Data Layer API** (`MessageClient`), on paths `/bp-log` and
`/med-log`. If no phone is currently connected (Bluetooth out of range,
phone off), the screen shows "Hata — Tekrar Dene" rather than silently
losing the entry — nothing is queued/retried automatically in this version.

## Known limitation (by design, for v1)

The medication screen doesn't know your actual medication list — it's
free-text (voice or keyboard) rather than a picker. Teaching the watch your
active medication list would mean building phone→watch sync too (via
`DataClient`, pushed whenever your medication list changes) — worth doing
as a v2 once this baseline round-trip is confirmed working.

## Build & install (no Android Studio required)

This project has no committed Gradle wrapper JAR (binary files can't be
generated in a text-only environment) — generate it once using a system
Gradle install:

```bash
# If you don't have `gradle` on PATH yet:
sdkmanager --install "cmdline-tools;latest"   # you likely already have this
sudo apt install gradle                       # or use sdkman: sdk install gradle

cd wear-app
gradle wrapper --gradle-version 8.9
```

That generates `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`.
From then on, use `./gradlew` for everything:

```bash
# Make sure your WATCH (not phone) is connected and in developer mode with
# ADB debugging enabled (Settings > Developer options > ADB debugging on
# the watch itself, or "Debug over Bluetooth" if using wireless ADB via the
# paired phone's Wear OS app).
adb devices    # confirm the watch shows up

./gradlew installDebug
```

If `adb devices` only shows your phone, not the watch: on the watch, enable
Developer options (Settings → About → tap Software version ~7 times), then
Settings → Developer options → turn on "ADB debugging" and "Debug over
Bluetooth". Wireless debugging to a watch usually goes through
`adb connect <watch-ip>:5555` once you enable Wi-Fi debugging on the watch,
since most watches don't have a USB port.

## Package name

Currently `com.yourname.healthtrackerwear` (matching the placeholder in the
phone app's `com.yourname.healthtracker`) — change both
`app/build.gradle.kts` (`namespace`, `applicationId`) and
`AndroidManifest.xml` if you want something real before a wider release;
doesn't matter for personal use.

## Language support (TR/EN)

`presentation/Strings.kt` holds a TR/EN dictionary; the currently selected
language lives in `MainMenuScreen.kt` (a `remember { mutableStateOf(...) }`
provided to every screen via `CompositionLocalProvider`) and persists across
launches via `data/LanguagePrefs.kt` (plain `SharedPreferences`, no need for
anything heavier on a 3-screen watch app). A "TR"/"EN" chip on the main menu
toggles it.

Note: the medication screen always sends `mealType` to the phone as `"Aç"`
or `"Tok"` regardless of the watch's display language — that's the exact
string the phone app's database and UI expect, so the *display* label is
translated but the *wire value* isn't.
