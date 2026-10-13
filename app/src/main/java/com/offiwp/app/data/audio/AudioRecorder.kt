package com.offiwp.app.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTimeMs: Long = 0L

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startRecording(): File? {
        stopRecordingSilently()
        return try {
            val outputFile = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
            currentFile = outputFile

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            startTimeMs = System.currentTimeMillis()
            startAmplitudeUpdates()
            outputFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            currentFile = null
            mediaRecorder = null
            null
        }
    }

    private fun startAmplitudeUpdates() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            while (mediaRecorder != null) {
                try {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    // Normalize amplitude between 0.0 and 1.0
                    val normalized = (maxAmp / 32767f).coerceIn(0f, 1f)
                    _amplitude.value = normalized
                    _recordingDurationMs.value = System.currentTimeMillis() - startTimeMs
                } catch (e: Exception) {
                    _amplitude.value = 0f
                }
                delay(100)
            }
        }
    }

    fun stopRecording(): Pair<File?, Long> {
        amplitudeJob?.cancel()
        val duration = System.currentTimeMillis() - startTimeMs
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _amplitude.value = 0f
            _recordingDurationMs.value = 0L
            Pair(currentFile, duration)
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping recording", e)
            stopRecordingSilently()
            Pair(null, 0L)
        }
    }

    fun cancelRecording() {
        amplitudeJob?.cancel()
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error cancelling recording", e)
        } finally {
            mediaRecorder = null
            currentFile?.delete()
            currentFile = null
            _amplitude.value = 0f
            _recordingDurationMs.value = 0L
        }
    }

    private fun stopRecordingSilently() {
        amplitudeJob?.cancel()
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        _amplitude.value = 0f
    }
}
