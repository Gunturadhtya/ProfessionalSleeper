package com.gntr.professionalsleeper.framework.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gntr.professionalsleeper.R
import com.gntr.professionalsleeper.framework.alarm.AlarmConstants.ALARM_CHANNEL_ID
import com.gntr.professionalsleeper.framework.alarm.AlarmConstants.EXTRA_SESSION_ID
import com.gntr.professionalsleeper.presentation.ringing.RingingActivity
import timber.log.Timber

class AlarmService: Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand dieksekusi")

        val sessionId = intent?.getIntExtra(EXTRA_SESSION_ID, -1) ?: -1
        if (sessionId == -1) {
            Timber.e("Gagal memproses alarm: EXTRA_SESSION_ID null atau -1")
            return START_NOT_STICKY
        }

        Timber.i("Mempersiapkan Notifikasi Alarm untuk sessionId: $sessionId")
        createNotificationChannel()

        val fullScreenIntent = Intent(this, RingingActivity::class.java).apply {
            putExtra(EXTRA_SESSION_ID, sessionId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            sessionId,
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

        Timber.d("Menjalankan startForeground dengan ID 1001")
        startForeground(1001, notification)

        Timber.i("Mencoba memaksa peluncuran RingingActivity dari background")
        try {
            startActivity(fullScreenIntent)
        } catch (e: Exception) {
            Timber.e(e, "Gagal memaksa buka UI Alarm (Restriksi OS terlalu ketat)")
        }

        return START_STICKY
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

    override fun onBind(p0: Intent?): IBinder? = null
}