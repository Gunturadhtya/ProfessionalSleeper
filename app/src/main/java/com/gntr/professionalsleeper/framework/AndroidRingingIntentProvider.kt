package com.gntr.professionalsleeper.framework

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.gntr.domain.alarm.IRingingIntentProvider
import com.gntr.ui.ringing.RingingActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class AndroidRingingIntentProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : IRingingIntentProvider {

    override fun getRingingPendingIntent(sessionId: Long): PendingIntent {
        val fullScreenIntent = Intent(context, RingingActivity::class.java).apply {
            putExtra(RingingActivity.EXTRA_SESSION_ID, sessionId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        return PendingIntent.getActivity(
            context,
            sessionId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun launchRingingActivity(sessionId: Long) {
        val launchIntent = Intent(context, RingingActivity::class.java).apply {
            putExtra(RingingActivity.EXTRA_SESSION_ID, sessionId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to explicitly launch RingingActivity")
        }
    }
}