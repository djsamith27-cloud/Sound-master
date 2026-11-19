# Swara Studio — Song Doctor & Mastering (Android)

An Android (Kotlin + Jetpack Compose) audio mastering, parametric EQ repair, AI Lyrics Doctor, and cloud song-sync app.

---

## Download APK from GitHub (සිංහල / English)

### 1. Direct APK Download (GitHub Releases / Actions)
- **GitHub Releases**: Repository එකේ දකුණු පැත්තේ **Releases** section එකට ගොස් **`SwaraStudio-release.apk`** download කර Android දුරකථනයේ install කරන්න. *(Go to **Releases** and download `SwaraStudio-release.apk`.)*
- **GitHub Actions (Latest Build)**: Repository එකේ **Actions** tab → **Build & Release Android APK** → නවතම workflow run එකේ **Artifacts** යටතේ **`SwaraStudio-APK`** download කරගත හැක.
- **Create a New Release on GitHub**:
  - Push a version tag (e.g., `git tag v1.0.0 && git push origin v1.0.0`), **or**
  - Open **Actions → Build & Release Android APK → Run workflow**, enter a tag such as `v1.0.0`, and click **Run workflow**. The `.github/workflows/build-apk.yml` pipeline will build `SwaraStudio-release.apk` and publish it to **Releases** automatically.

### 2. Download Source & Build Locally
- **Requirements**: JDK 21, Android SDK (API 36), Gradle 9.3.1 (or Android Studio).
- Decode the signing keystore before building:
  ```bash
  base64 --decode debug.keystore.base64 > debug.keystore
  gradle assembleDebug
  ```
- Output APK: `app/build/outputs/apk/debug/app-debug.apk` (or `gradle assembleRelease` for `app/build/outputs/apk/release/app-release.apk`).

---

## Features
- **On-Device Audio Decoding**: Imports MP3, WAV, M4A, AAC, FLAC, and OGG files via Android Storage Access Framework (`OpenDocument`) with `MediaExtractor` + `MediaCodec`.
- **On-Device DSP Audit**: Analyzes Peak/RMS dBFS, 250 Hz low-mid mud, 3.2 kHz vocal presence, 6.8 kHz hi-hat/sibilance harshness, and streaming headroom.
- **Real-Time Mastering DSP Chain**:
  - 6-Band Parametric Repair EQ (Sub-Tight High-Pass, 90 Hz Bass Warmth, 250 Hz Mud Cut, 3.2 kHz Vocal Clarity, 6.8 kHz De-Esser, 11 kHz Studio Air)
  - Soft-knee Dynamics Compressor with live Gain Reduction (GR) metering
  - Mid-Side Stereo Width control & Output Makeup Gain
  - -0.6 dBFS Safety Limiter ceiling
- **Interactive Visualizers & A/B Comparison**: 140-bar interactive waveform scrubber, 40-bar logarithmic live FFT spectrum analyzer, and instant A (Original) / B (Fixed Master) toggle during `AudioTrack` playback.
- **AI Lyrics Doctor**: Fixes spelling/grammar/rhythm issues in pasted lyrics or extracts and repairs lyrics from the loaded audio using Firebase AI Logic (`gemini-3.8-flash`) with App Check protection.
- **Cloud Lyrics Sync**: Google Sign-In (`CredentialManager`) and Firebase Firestore persistence to save and restore modified lyrics per song under `/users/{userId}/songs/{songId}`.
- **Presets & Bilingual UI**: Built-in mastering presets (`Recommended Fix`, `Trap & Rap Punch`, `Vocal Clarity`, `Flat`) and instant Sinhala (`සිංහල`) / English (`EN`) language switching.
- **WAV Export**: Offline DSP rendering to 16-bit PCM WAV via Android `CreateDocument`.