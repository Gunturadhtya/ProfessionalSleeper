package com.gntr.framework.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.core.net.toUri
import com.gntr.domain.alarm.IAudioPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class AudioPlayerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IAudioPlayer {

    private var mediaPlayer: MediaPlayer? = null

    override fun play(uriString: String) {
        try {
            val uri = uriString.toUri()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize or play MediaPlayer for alarm ringtone.")
        }
    }

    override fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
        }
    }

    override fun release() {
        stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}