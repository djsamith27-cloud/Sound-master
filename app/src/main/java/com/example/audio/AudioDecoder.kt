package com.example.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder

object AudioDecoder {

    suspend fun decodeUri(context: Context, uri: Uri): DecodedAudio = withContext(Dispatchers.IO) {
        val fileName = resolveFileName(context, uri)
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var trackFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    trackFormat = fmt
                    break
                }
            }
            require(audioTrackIndex >= 0 && trackFormat != null) {
                "No audio track found in selected file."
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)!!
            var sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var pcmEncoding = if (trackFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                trackFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else {
                AudioFormat.ENCODING_PCM_16BIT
            }

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(trackFormat, null, null, 0)
            codec.start()

            val outChannels = channels.coerceIn(1, 2)
            val leftChunks = ArrayList<FloatArray>()
            val rightChunks = ArrayList<FloatArray>()
            var totalFrames = 0

            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val inputBuf = codec.getInputBuffer(inIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            val pts = extractor.sampleTime
                            codec.queueInputBuffer(inIndex, 0, sampleSize, pts, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFmt = codec.outputFormat
                        if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channels = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        if (outFmt.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            pcmEncoding = outFmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }
                    }

                    outIndex >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIndex)
                        if (outBuf != null && bufferInfo.size > 0) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            outBuf.order(ByteOrder.LITTLE_ENDIAN)

                            val srcCh = channels.coerceAtLeast(1)
                            when (pcmEncoding) {
                                AudioFormat.ENCODING_PCM_FLOAT -> {
                                    val floatBuf = outBuf.asFloatBuffer()
                                    val totalSamples = floatBuf.remaining()
                                    val frames = totalSamples / srcCh
                                    if (frames > 0) {
                                        val lChunk = FloatArray(frames)
                                        val rChunk = if (outChannels >= 2) FloatArray(frames) else null
                                        for (f in 0 until frames) {
                                            val lVal = floatBuf.get()
                                            lChunk[f] = lVal.coerceIn(-1f, 1f)
                                            if (srcCh >= 2) {
                                                val rVal = floatBuf.get()
                                                if (rChunk != null) rChunk[f] = rVal.coerceIn(-1f, 1f)
                                                for (extra in 2 until srcCh) floatBuf.get()
                                            }
                                        }
                                        leftChunks.add(lChunk)
                                        if (rChunk != null) rightChunks.add(rChunk)
                                        totalFrames += frames
                                    }
                                }

                                else -> {
                                    // Standard 16-bit signed PCM
                                    val shortBuf = outBuf.asShortBuffer()
                                    val totalSamples = shortBuf.remaining()
                                    val frames = totalSamples / srcCh
                                    if (frames > 0) {
                                        val lChunk = FloatArray(frames)
                                        val rChunk = if (outChannels >= 2) FloatArray(frames) else null
                                        for (f in 0 until frames) {
                                            lChunk[f] = (shortBuf.get() / 32768f).coerceIn(-1f, 1f)
                                            if (srcCh >= 2) {
                                                val rVal = (shortBuf.get() / 32768f).coerceIn(-1f, 1f)
                                                if (rChunk != null) rChunk[f] = rVal
                                                for (extra in 2 until srcCh) shortBuf.get()
                                            }
                                        }
                                        leftChunks.add(lChunk)
                                        if (rChunk != null) rightChunks.add(rChunk)
                                        totalFrames += frames
                                    }
                                }
                            }
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            require(totalFrames > 0) { "Decoded audio contains no samples." }

            val leftArray = FloatArray(totalFrames)
            var offset = 0
            for (chunk in leftChunks) {
                chunk.copyInto(leftArray, offset)
                offset += chunk.size
            }

            val channelSamples = if (outChannels >= 2 && rightChunks.isNotEmpty()) {
                val rightArray = FloatArray(totalFrames)
                var rOffset = 0
                for (chunk in rightChunks) {
                    chunk.copyInto(rightArray, rOffset)
                    rOffset += chunk.size
                }
                arrayOf(leftArray, rightArray)
            } else {
                arrayOf(leftArray)
            }

            val durationSec = totalFrames.toFloat() / sampleRate.toFloat()
            val audit = AudioAuditEngine.analyze(leftArray, sampleRate)

            DecodedAudio(
                fileName = fileName,
                sampleRate = sampleRate,
                channels = channelSamples.size,
                durationSec = durationSec,
                channelSamples = channelSamples,
                peaks = audit.peaks,
                peakDb = audit.peakDb,
                rmsDb = audit.rmsDb,
                issues = audit.issues
            )
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (_: Throwable) {
            }
            try {
                extractor.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun resolveFileName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "audio_track"
    }
}
