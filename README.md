# OriginCTS

[![Android 15+](https://img.shields.io/badge/Android-15%20%7C%2016-green.svg)](https://developer.android.com)
[![OriginOS](https://img.shields.io/badge/OriginOS-5%20%7C%206-blue.svg)](https://www.vivo.com)
[![Xposed Module](https://img.shields.io/badge/LSPosed-Zygisk-orange.svg)](https://github.com/LSPosed/LSPosed)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Native **Circle to Search (CTS)**, **Live Translate**, and **Gemini / Google Assistant** integration for Vivo and iQOO devices running **OriginOS 5** and **OriginOS 6** (Android 15 / 16).

---

## 🌟 Key Features

### 1. Circle to Search (CTS)
* **Native Gestures:** Long-press Home button or center navigation handle/pill to trigger Circle to Search immediately.
* **Navigation Bar & UpSlide Integration:** Hooks directly into `com.vivo.upslide` and `SystemUI` without requiring third-party accessibility overlays or simulated touches.
* **Quick Settings Tile:** Includes a dedicated CTS tile in the notification shade.

### 2. Live Translate (Scroll and Translate)
* **Full-Page Translation:** Re-enables Google's continuous scrolling translation bar inside Circle to Search.
* **MediaProjection Screen-Flicker Fix:** Hooks system compositor (`SurfaceFlinger` / `WindowStateAnimator`) to flag Google's `TYPE_APPLICATION_OVERLAY` with `SKIP_SCREENSHOT`. Prevents recursive screen mirroring and eliminates overlay flickering when sharing the full display.
* **Dynamic Symbol Resolution:** Utilizes [DexKit](https://github.com/LuckyPray/DexKit) at runtime to locate obfuscated Google App entrypoints across APK updates.

### 3. Gemini / Assistant Power Button Shortcut
* **0.5s Power Key Hold:** Opens Google Assistant or Gemini directly while keeping the long-press power menu fully functional.

### 4. Voice Wakeup & Native Voice Match
* **Wakeup Redirection:** Intercepts OriginOS native Jovi / Xiao V voice trigger (`com.vivo.agent.action.wakeup_by_voice`) and seamlessly launches Gemini or Google Assistant.
* **Native Hotword Bypass:** Bypasses `MANAGE_HOTWORD_DETECTION` and `MANAGE_VOICE_KEYPHRASES` permission enforcement in `VoiceInteractionManagerService`, allowing Google App to register hotword detection and train Voice Match without requiring system priv-app modification.

### 5. Resilient Architecture
* **Cross-Process Global State:** Feature flags persist in `Settings.Global` (`origincts_features`), ensuring hook states remain active even when OriginOS aggressive background process freezer suspends module components.

---

## 📋 Requirements

* **Device:** Vivo / iQOO device running OriginOS 5 or OriginOS 6 (Android 15 / 16, `arm64-v8a`).
* **Root Solution:** KernelSU, APatch, or Magisk.
* **Xposed Framework:** LSPosed (Zygisk release recommended).
* **Target App:** Official Google App (`com.google.android.googlequicksearchbox`).

---

## 🚀 Installation & Setup

1. **Install OriginCTS APK:**
   Download the latest APK release and install it on your device.

2. **Enable in LSPosed:**
   Open **LSPosed**, enable **OriginCTS**, and select the following scopes:
   * `System Framework` (`android`)
   * `System UI` (`com.android.systemui`)
   * `UpSlide` (`com.vivo.upslide`)
   * `Google` (`com.google.android.googlequicksearchbox`)

3. **Grant Secure Settings Permission:**
   Execute via ADB or root shell (if prompted):
   ```bash
   pm grant io.github.origincts android.permission.WRITE_SECURE_SETTINGS
   ```

4. **Restart System Server / Framework:**
   Restart your framework or soft reboot to allow `system_server` hooks to bind:
   ```bash
   su -c "stop && start"
   ```

5. **Configure Default Assistant:**
   Go to **Settings** → **Apps** → **Default apps** → **Digital assistant app** → Select **Google**.

---

## ⚙️ Module Settings

Launch **OriginCTS** from launcher or LSPosed module list:

| Setting | Description |
| :--- | :--- |
| **Use OriginCTS** | Master switch to enable or disable all module hooks. |
| **Circle to Search** | Enables long-press gesture on Home / navigation pill. |
| **Gemini** | Enables 0.5s power button hold to summon Gemini. |
| **Voice Wakeup (Hey Google)** | Enables voice wakeup redirection and native hotword bypass. |
| **Scroll and translate** | Enables full-page live translation in CTS. |
| **Voice Match Settings** | Shortcut directly into Google Assistant Voice Match enrollment. |
| **Hide app icon** | Hides launcher icon (accessible anytime via LSPosed). |

---

## 🛠️ Build from Source

### Prerequisites
* JDK 17
* Android SDK (API Level 35/36)
* NDK (arm64-v8a)

### Build Commands
```bash
git clone https://github.com/bonelag/origincts.git
cd origincts

# Run unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug

# Assemble Release APK
./gradlew assembleRelease
```

Compiled APK will be located in:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License & Attributions

* OriginCTS source code is licensed under the [Apache License 2.0](LICENSE).
* Uses [DexKit](https://github.com/LuckyPray/DexKit) (Apache-2.0 / LGPL-3.0).
* See [third-party-notices.txt](app/src/main/assets/third-party-notices.txt) for license notices of included libraries and dependencies.
