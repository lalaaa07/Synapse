package com.example.synapse

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

private const val TAG = "MusicManager"

/**
 * Controls playback of the local calming audio track.
 * Auto-routes to a connected Bluetooth audio device if one is active,
 * otherwise plays through the phone's normal audio output (handled
 * automatically by Android's audio routing — no extra code needed).
 */
class MusicManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    fun play() {
        if (mediaPlayer?.isPlaying == true) {
            Log.d(TAG, "Already playing, ignoring play() call")
            return
        }

        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.calming_music).apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                start()
            }
            Log.d(TAG, "Calming music started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start calming music: ${e.message}", e)
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        Log.d(TAG, "Calming music stopped")
    }

    fun setVolume(volume: Float) {
        // volume expected in range 0.0–1.0
        mediaPlayer?.setVolume(volume, volume)
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun release() {
        stop()
    }
}