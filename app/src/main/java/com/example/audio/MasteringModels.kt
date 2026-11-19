package com.example.audio

enum class AppLanguage {
    SINHALA,
    ENGLISH
}

data class MasteringParams(
    val hpf: Float = 34f,
    val low: Float = -1.0f,
    val mud: Float = -2.6f,
    val pres: Float = 2.8f,
    val deess: Float = -2.4f,
    val air: Float = 1.8f,
    val thresh: Float = -15f,
    val ratio: Float = 2.8f,
    val width: Float = 1.08f,
    val gain: Float = 1.5f
)

data class MasteringPreset(
    val id: String,
    val name: String,
    val params: MasteringParams
)

val MASTERING_PRESETS = listOf(
    MasteringPreset(
        id = "rec",
        name = "Recommended Fix",
        params = MasteringParams(
            hpf = 34f, low = -1.0f, mud = -2.6f, pres = 2.8f,
            deess = -2.4f, air = 1.8f, thresh = -15f, ratio = 2.8f, width = 1.08f, gain = 1.5f
        )
    ),
    MasteringPreset(
        id = "trap",
        name = "Trap & Rap Punch",
        params = MasteringParams(
            hpf = 32f, low = 1.2f, mud = -3.0f, pres = 2.2f,
            deess = -1.8f, air = 2.0f, thresh = -16f, ratio = 3.2f, width = 1.12f, gain = 1.8f
        )
    ),
    MasteringPreset(
        id = "vocal",
        name = "Vocal Clarity",
        params = MasteringParams(
            hpf = 42f, low = -1.5f, mud = -3.2f, pres = 3.6f,
            deess = -2.8f, air = 2.4f, thresh = -14f, ratio = 2.5f, width = 0.96f, gain = 1.6f
        )
    ),
    MasteringPreset(
        id = "flat",
        name = "Flat (Reset)",
        params = MasteringParams(
            hpf = 20f, low = 0f, mud = 0f, pres = 0f,
            deess = 0f, air = 0f, thresh = -6f, ratio = 1.5f, width = 1.0f, gain = 0f
        )
    )
)

data class AuditIssue(
    val titleSi: String,
    val detailSi: String,
    val titleEn: String,
    val detailEn: String
)

class DecodedAudio(
    val fileName: String,
    val sampleRate: Int,
    val channels: Int,
    val durationSec: Float,
    val channelSamples: Array<FloatArray>,
    val peaks: List<Float>,
    val peakDb: Float,
    val rmsDb: Float,
    val issues: List<AuditIssue>
) {
    val totalFrames: Int
        get() = channelSamples.firstOrNull()?.size ?: 0
}

object StudioStrings {
    fun uploadBtn(lang: AppLanguage) = if (lang == AppLanguage.SINHALA) {
        "ගීතයක් Upload කරන්න (MP3/WAV)"
    } else {
        "Upload Song (MP3/WAV)"
    }

    fun selectTrackPlaceholder(lang: AppLanguage) = if (lang == AppLanguage.SINHALA) {
        "ගීතයක් තෝරන්න..."
    } else {
        "Select a song..."
    }

    fun dropTitle(lang: AppLanguage) = if (lang == AppLanguage.SINHALA) {
        "ඔබේ MP3 / WAV ගීතය මෙතනට තෝරන්න (Tap කරන්න)"
    } else {
        "Tap to Select Your MP3 / WAV Song"
    }

    fun dropSubtitle(lang: AppLanguage) = if (lang == AppLanguage.SINHALA) {
        "දුරකථනය ඇතුළේම අඩුපාඩු පරීක්ෂා කර EQ / Compressor යොදා WAV ලෙස save කළ හැක."
    } else {
        "Audit audio flaws on-device and master with Parametric EQ, Compressor & Limiter to WAV."
    }

    fun btnOriginal(lang: AppLanguage) = if (lang == AppLanguage.SINHALA) {
        "A: මුල් ගීතය"
    } else {
        "A: Original"
    }

    fun btnFixed(lang: AppLanguage) = "B: Fixed Master"

    fun exportBtn(lang: AppLanguage, isExporting: Boolean): String {
        if (isExporting) {
            return if (lang == AppLanguage.SINHALA) "සකසමින්..." else "Rendering..."
        }
        return if (lang == AppLanguage.SINHALA) "හැදූ WAV ගන්න" else "Export Fixed WAV"
    }

    fun waveformLabel(lang: AppLanguage) = if (lang == AppLanguage.SINHALA) {
        "Waveform (Touch කර අවශ්‍ය තැනට යන්න)"
    } else {
        "Waveform (Tap or Drag to Seek)"
    }

    fun spectrumLabel() = "Live Spectrum"

    fun auditHeading(lang: AppLanguage) = if (lang == AppLanguage.SINHALA) {
        "හඳුනාගත් Audio අඩුපාඩු (On-Device DSP Audit)"
    } else {
        "Detected Audio Flaws (On-Device DSP Audit)"
    }

    fun auditEmptyPrompt(lang: AppLanguage) = if (lang == AppLanguage.SINHALA) {
        "ගීතයක් upload කළ පසු සංඛ්‍යාත සහ ශබ්ද අඩුපාඩු මෙහි දිස්වේ."
    } else {
        "Frequency balance and loudness flaws will appear here after loading a track."
    }

    fun auditSummary(lang: AppLanguage, peakDb: Float, rmsDb: Float): String {
        val peakStr = String.format(java.util.Locale.US, "%.1f", peakDb)
        val rmsStr = String.format(java.util.Locale.US, "%.1f", rmsDb)
        return if (lang == AppLanguage.SINHALA) {
            "DSP පරීක්ෂාව සම්පූර්ණයි — Peak $peakStr dBFS | RMS $rmsStr dBFS"
        } else {
            "DSP Audit Complete — Peak $peakStr dBFS | RMS $rmsStr dBFS"
        }
    }

    fun auditHealthy(lang: AppLanguage) = if (lang == AppLanguage.SINHALA) {
        "ගීතයේ සංඛ්‍යාත සමබරතාවය හොඳ මට්ටමක ඇත."
    } else {
        "Track frequency balance and headroom look healthy."
    }

    fun decodingTrack(lang: AppLanguage) = if (lang == AppLanguage.SINHALA) {
        "ගීතය පරීක්ෂා කරමින් පවතී..."
    } else {
        "Decoding & auditing audio..."
    }

    fun exportSuccess(lang: AppLanguage) = if (lang == AppLanguage.SINHALA) {
        "Master කළ WAV ගොනුව සාර්ථකව Save කරන ලදී!"
    } else {
        "Mastered WAV file saved successfully!"
    }
}
