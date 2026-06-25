package com.gntr.framework.alarm

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.gntr.domain.alarm.IAudioPlayer
import com.gntr.domain.alarm.IRingingIntentProvider
import com.gntr.domain.repository.IPreferencesRepository
import com.gntr.framework.alarm.AlarmConstants.EXTRA_SESSION_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AlarmService : Service() {

    companion object {
        const val ACTION_START_RINGING = "com.gntr.professionalsleeper.ACTION_START_RINGING"
    }

    @Inject
    lateinit var prefsRepo: IPreferencesRepository

    @Inject
    lateinit var audioPlayer: IAudioPlayer

    @Inject
    lateinit var notificationFactory: AlarmNotificationFactory

    @Inject
    lateinit var ringingIntentProvider: IRingingIntentProvider

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_RINGING) {
            serviceScope.launch {
                delay(450L)
                val uriString = prefsRepo.alarmRingtoneUriFlow.first()
                audioPlayer.play(uriString)
            }
            return START_STICKY
        }

        val sessionId = intent?.getLongExtra(EXTRA_SESSION_ID, -1L) ?: -1L
        if (sessionId == -1L) {
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            Timber.d("canUseFullScreenIntent() = ${notificationManager.canUseFullScreenIntent()}")
        }

        val notification = notificationFactory.createNotification(sessionId, "Wake Up", "Polyphasic Alarm")

        try {
            startForeground(1001, notification)
            Timber.d("startForeground succeeded for sessionId=$sessionId")

            ringingIntentProvider.launchRingingActivity(sessionId)

        } catch (e: Exception) {
            Timber.e(e, "startForeground or launch failed for sessionId=$sessionId")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.release()
        serviceScope.cancel()
    }

    override fun onBind(p0: Intent?): IBinder? = null
}