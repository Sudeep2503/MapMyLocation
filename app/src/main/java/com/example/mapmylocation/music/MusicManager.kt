package com.example.mapmylocation.music

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import com.example.mapmylocation.weather.WeatherCategory
import kotlin.concurrent.thread

class MusicManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlaylist: List<TrackInfo> = emptyList()
    private var currentTrackIndex: Int = 0
    private var isPaused: Boolean = false

    // Fallback synth track if MediaPlayer ever encounters device-specific decoder restrictions
    private var fallbackAudioTrack: AudioTrack? = null
    private var isFallbackPlaying: Boolean = false

    var onStatusChanged: ((status: String, trackTitle: String, categoryName: String, isPlaying: Boolean) -> Unit)? = null

    fun playForWeatherCategory(category: WeatherCategory) {
        val tracks = MusicRecommendation.getTracksForWeather(category)
        if (tracks.isEmpty()) return

        currentPlaylist = tracks
        currentTrackIndex = 0
        playCurrentTrack()
    }

    fun playCurrentTrack() {
        if (currentPlaylist.isEmpty()) return

        val track = currentPlaylist[currentTrackIndex]
        stop()

        try {
            val player = MediaPlayer.create(context, track.resId)
            if (player != null) {
                mediaPlayer = player
                player.setVolume(1.0f, 1.0f)
                player.isLooping = true
                player.setOnCompletionListener {
                    skipNext()
                }
                player.setOnErrorListener { _, _, _ ->
                    startFallbackAudio(track)
                    true
                }
                player.start()
                isPaused = false
                onStatusChanged?.invoke("Playing", track.title, track.categoryName, true)
            } else {
                startFallbackAudio(track)
            }
        } catch (_: Exception) {
            startFallbackAudio(track)
        }
    }

    private fun startFallbackAudio(track: TrackInfo) {
        stopFallbackAudio()
        isFallbackPlaying = true

        val sampleRate = 22050
        val freq = when (track.weatherCategory) {
            WeatherCategory.SUNNY -> 523.25  // C5 (Upbeat)
            WeatherCategory.CLOUDY -> 440.00 // A4 (Chill)
            WeatherCategory.RAINY -> 349.23  // F4 (Relaxing)
            WeatherCategory.STORMY -> 293.66 // D4 (Calm)
            WeatherCategory.SNOWY -> 261.63  // C4 (Peaceful)
            else -> 440.00
        }

        thread(name = "FallbackAudioThread") {
            try {
                val minSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minSize * 2)
                    .build()

                fallbackAudioTrack = audioTrack
                audioTrack.play()

                val numSamples = sampleRate * 2
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val sample = Math.sin(2.0 * Math.PI * freq * t)
                    buffer[i] = (sample * 16000).toInt().toShort()
                }

                while (isFallbackPlaying) {
                    audioTrack.write(buffer, 0, buffer.size)
                }
            } catch (_: Exception) {
            }
        }

        onStatusChanged?.invoke("Playing (Audio Synth)", track.title, track.categoryName, true)
    }

    private fun stopFallbackAudio() {
        isFallbackPlaying = false
        try {
            fallbackAudioTrack?.stop()
            fallbackAudioTrack?.release()
        } catch (_: Exception) {
        } finally {
            fallbackAudioTrack = null
        }
    }

    fun togglePlayPause() {
        if (mediaPlayer == null && !isFallbackPlaying && currentPlaylist.isNotEmpty()) {
            playCurrentTrack()
            return
        }

        val track = currentPlaylist.getOrNull(currentTrackIndex) ?: return

        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            isPaused = true
            onStatusChanged?.invoke("Paused", track.title, track.categoryName, false)
        } else if (isFallbackPlaying) {
            stopFallbackAudio()
            isPaused = true
            onStatusChanged?.invoke("Paused", track.title, track.categoryName, false)
        } else if (isPaused) {
            if (mediaPlayer != null) {
                mediaPlayer?.start()
            } else {
                startFallbackAudio(track)
            }
            isPaused = false
            onStatusChanged?.invoke("Playing", track.title, track.categoryName, true)
        } else {
            playCurrentTrack()
        }
    }

    fun stop() {
        stopFallbackAudio()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
            isPaused = false
        }

        val track = currentPlaylist.getOrNull(currentTrackIndex)
        val title = track?.title ?: "No track selected"
        val category = track?.categoryName ?: "None"
        onStatusChanged?.invoke("Stopped", title, category, false)
    }

    fun skipNext() {
        if (currentPlaylist.isEmpty()) return
        currentTrackIndex = (currentTrackIndex + 1) % currentPlaylist.size
        playCurrentTrack()
    }

    fun skipPrevious() {
        if (currentPlaylist.isEmpty()) return
        currentTrackIndex = if (currentTrackIndex - 1 < 0) currentPlaylist.size - 1 else currentTrackIndex - 1
        playCurrentTrack()
    }

    fun getCurrentTrack(): TrackInfo? {
        return currentPlaylist.getOrNull(currentTrackIndex)
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true || isFallbackPlaying
    }

    fun release() {
        stop()
    }
}
