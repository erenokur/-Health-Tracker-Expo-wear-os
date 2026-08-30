# Health Tracker (Wear OS Companion App)

A standalone Kotlin and Jetpack Compose (Wear Compose) application for Wear OS smartwatches. It serves as a companion to the main Health Tracker mobile app, allowing you to log blood pressure and medications directly from your wrist.

## Features

- **Blood Pressure Logging**: Quickly input your systolic, diastolic, and pulse readings using standard Wear OS rotary controls/number pads.
- **Medication Logging**: Select from your active medication list (synced from the phone) and mark them as taken (Fasting / Fed).
- **Instant Sync**: Uses the Wearable Data Layer API to instantly transmit your logs to the paired phone for persistent storage.

## Getting Started

### Prerequisites

- Java (JDK 17)
- Android SDK (with Wear OS emulator or a physical watch)
- Google Play Services (Wearable) present on the watch (Standard on Wear OS 3+).

### Build Instructions

You do not need Android Studio to build the APK. You can build it directly via the included Gradle wrapper.

1. Ensure the wrapper is executable:
   ```bash
   chmod +x ./gradlew
   ```
2. Build a Debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Build a Release APK:
   ```bash
   ./gradlew assembleRelease
   ```

### Installing on a Physical Watch

Since most watches don't have USB ports, you'll need to use Wireless ADB Debugging:

1. Enable **Developer Options** on your watch (Settings -> About -> Tap Software Version 7 times).
2. Go to Developer Options and enable **ADB Debugging** and **Wireless Debugging**.
3. Note the IP address and port displayed under Wireless Debugging (e.g., `192.168.1.100:5555`).
4. Connect to the watch from your terminal:
   ```bash
   adb connect <watch-ip>:<port>
   ```
5. Install the built APK:
   ```bash
   adb install -r app/build/outputs/apk/release/app-release-unsigned.apk
   ```

## Continuous Integration (CI/CD)

The repository includes a GitHub Actions workflow (`.github/workflows/build-wear-apk.yml`). 
Whenever code is pushed to the `release` branch, it automatically builds a Release APK and attaches it to a new GitHub Release.
