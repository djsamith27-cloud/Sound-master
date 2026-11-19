# Swara Studio — Song Doctor & Mastering (Android)

An Android (Kotlin + Jetpack Compose) audio mastering, parametric EQ repair, AI Lyrics Doctor, and cloud song-sync app.

[![Download Swara Studio APK](https://img.shields.io/badge/Download-Swara_Studio_APK_(19_MB)-10b981?style=for-the-badge&logo=android&logoColor=white)](./apk/SwaraStudio.apk?raw=true)

---

## Download App from GitHub (සිංහල / English)

### 1. Direct APK Download (1-Click Install)
- **Direct Download**: **[`Download SwaraStudio.apk`](./apk/SwaraStudio.apk?raw=true)** *(Click කළ විට සෘජුවම APK එක download වේ.)*
- **View File on GitHub**: **[`apk/SwaraStudio.apk`](./apk/SwaraStudio.apk)** *(File පිටුවේ ඇති **View raw** / Download button එක click කර Android දුරකථනයේ install කරන්න.)*
- **GitHub Releases**: Repository එකේ **Releases** section එකට ගොස් **`SwaraStudio-release.apk`** ඇත්නම් එයද download කළ හැක.

### 2. Optional: Auto-Build APK via GitHub Actions / Releases
- **Option A — Upload to GitHub Releases**: AI Studio **Settings → Generate APK** හෝ `./apk/SwaraStudio.apk` file එක GitHub **Releases → Draft a new release** වෙත upload කරන්න.
- **Option B — Auto-Build Workflow**: GitHub web UI එකේ **Add file → Create new file** මගින් `.github/workflows/build-apk.yml` සාදා `docs/github-release-workflow.yml` හි අන්තර්ගතය paste කර commit කරන්න. ඉන්පසු **Actions → Build & Release Android APK** මගින් ස්වයංක්‍රීයව APK build කර **Releases / Artifacts** හරහා download කළ හැක.

### 3. Download Source & Build Locally
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