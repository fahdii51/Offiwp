package com.offiwp.app.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
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

class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingUri: String? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playAudio(audioUriOrPath: String) {
        if (currentPlayingUri == audioUriOrPath && mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) {
                pause()
            } else {
                resume()
            }
            return
        }

        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                if (audioUriOrPath.startsWith("content://") || audioUriOrPath.startsWith("file://")) {
                    setDataSource(context, Uri.parse(audioUriOrPath))
                } else {
                    setDataSource(audioUriOrPath)
                }
                prepare()
                start()
                _duration.value = this.duration.toLong()
                _isPlaying.value = true
                currentPlayingUri = audioUriOrPath

                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPosition.value = 0L
                    progressJob?.cancel()
                }
            }
            startProgressTracker()
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error playing audio file: $audioUriOrPath", e)
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
            progressJob?.cancel()
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error pausing playback", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressTracker()
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error resuming playback", e)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _currentPosition.value = positionMs
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error seeking", e)
        }
    }

    fun stop() {
        progressJob?.cancel()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error stopping playback", e)
        } finally {
            mediaPlayer = null
            currentPlayingUri = null
            _isPlaying.value = false
            _currentPosition.value = 0L
            _duration.value = 0L
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (mediaPlayer != null && _isPlaying.value) {
                try {
                    _currentPosition.value = mediaPlayer?.currentPosition?.toLong() ?: 0L
                } catch (_: Exception) {}
                delay(100)
            }
        }
    }
}
