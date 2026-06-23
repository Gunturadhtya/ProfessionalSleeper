package com.gntr.professionalsleeper.framework.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

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

    fun getRawLauncherActivities(context: Context): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return context.packageManager.queryIntentActivities(intent, 0)
            .sortedBy { it.activityInfo.packageName }
    }

    fun mapToAppInfo(context: Context, resolveInfos: List<ResolveInfo>): List<AppInfo> {
        val packageManager = context.packageManager
        return resolveInfos.map { resolveInfo ->
            AppInfo(
                appName = resolveInfo.loadLabel(packageManager).toString(),
                packageName = resolveInfo.activityInfo.packageName
            )
        }
    }
}