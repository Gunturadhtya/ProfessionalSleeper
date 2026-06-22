package com.gntr.professionalsleeper.framework.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object AppLauncherHelper {
    fun launchApp(context: Context, targetPackageName: String?): Boolean {
        if (targetPackageName.isNullOrEmpty()) return false

        val packageManager: PackageManager = context.packageManager
        val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(targetPackageName)

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            false
        }
    }

    fun getInstalledApps(context: Context): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = context.packageManager.queryIntentActivities(intent, 0)

        return resolveInfoList.map { resolveInfo ->
            AppInfo(
                appName = resolveInfo.loadLabel(context.packageManager).toString(),
                packageName = resolveInfo.activityInfo.packageName
            )
        }.sortedBy { it.appName }
    }
}