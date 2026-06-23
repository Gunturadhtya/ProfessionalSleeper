package com.gntr.professionalsleeper.framework.alarm

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gntr.professionalsleeper.R
import com.gntr.professionalsleeper.data.local.datastore.AppPreferencesRepository
import com.gntr.professionalsleeper.framework.alarm.AlarmConstants.ALARM_CHANNEL_ID
import com.gntr.professionalsleeper.framework.alarm.AlarmConstants.EXTRA_SESSION_ID
import com.gntr.professionalsleeper.presentation.ringing.RingingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import androidx.core.net.toUri

@AndroidEntryPoint
class AlarmService : Service() {

    @Inject
    lateinit var prefsRepo: AppPreferencesRepository

    private var mediaPlayer: MediaPlayer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @SuppressLint("FullScreenIntentPolicy")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sessionId = intent?.getLongExtra(EXTRA_SESSION_ID, -1L) ?: -1L
        if (sessionId == -1L) {
            return START_NOT_STICKY
        }

        createNotificationChannel()

        val fullScreenIntent = Intent(this, RingingActivity::class.java).apply {
            putExtra(EXTRA_SESSION_ID, sessionId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            sessionId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)

        playAlarmAudio()

        return START_STICKY
    }

    private fun playAlarmAudio() {
        serviceScope.launch {
            try {
                val uriString = prefsRepo.alarmRingtoneUriFlow.first()
                val uri = uriString.toUri()

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, uri)
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
    }

    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.d("Mendaftarkan NotificationChannel $ALARM_CHANNEL_ID")
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_description)
                setBypassDnd(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        serviceScope.cancel()
    }

    override fun onBind(p0: Intent?): IBinder? = null
}