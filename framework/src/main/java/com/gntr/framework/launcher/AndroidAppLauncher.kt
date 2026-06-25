package com.gntr.framework.launcher

import android.content.Context
import android.content.Intent
import com.gntr.domain.alarm.IAppLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidAppLauncher @Inject constructor(
    @ApplicationContext private val context: Context
) : IAppLauncher {

    override fun launchTargetApp(packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) return false

        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            false
        }
    }
}