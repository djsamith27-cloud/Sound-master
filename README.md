# Swara Studio — Song Doctor & Mastering (Android)

An Android (Kotlin + Jetpack Compose) audio mastering, parametric EQ repair, and on-device DSP flaw analyzer app.

## Features
- **On-Device Audio Decoding**: Imports MP3, WAV, M4A, AAC, FLAC, and OGG files via Android Storage Access Framework (`OpenDocument`) with `MediaExtractor` + `MediaCodec`.
- **On-Device DSP Audit**: Analyzes Peak/RMS dBFS, 250 Hz low-mid mud, 3.2 kHz vocal presence, 6.8 kHz hi-hat/sibilance harshness, and streaming headroom.
- **Real-Time Mastering DSP Chain**:
  - 6-Band Parametric Repair EQ (Sub-Tight High-Pass, 90 Hz Bass Warmth, 250 Hz Mud Cut, 3.2 kHz Vocal Clarity, 6.8 kHz De-Esser, 11 kHz Studio Air)
  - Soft-knee Dynamics Compressor with live Gain Reduction (GR) metering
  - Mid-Side Stereo Width control & Output Makeup Gain
  - -0.6 dBFS Safety Limiter ceiling
- **Interactive Visualizers & A/B Comparison**: 140-bar interactive waveform scrubber, 40-bar logarithmic live FFT spectrum analyzer, and instant A (Original) / B (Fixed Master) toggle during `AudioTrack` playback.
- **Presets & Bilingual UI**: Built-in mastering presets (`Recommended Fix`, `Trap & Rap Punch`, `Vocal Clarity`, `Flat`) and instant Sinhala (`සිංහල`) / English (`EN`) language switching.
- **WAV Export**: Offline DSP rendering to 16-bit PCM WAV via Android `CreateDocument`.