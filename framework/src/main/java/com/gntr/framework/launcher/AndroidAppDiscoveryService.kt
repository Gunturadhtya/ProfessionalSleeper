package com.gntr.framework.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import com.gntr.domain.alarm.IAppDiscoveryService
import com.gntr.domain.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAppDiscoveryService @Inject constructor(
    @ApplicationContext private val context: Context
) : IAppDiscoveryService {

    private var cachedResolveInfos: List<ResolveInfo>? = null

    override suspend fun getInstalledApps(pageIndex: Int, pageSize: Int): List<AppInfo> = withContext(Dispatchers.IO) {
        val allApps = getOrFetchRawApps()

        if (pageIndex >= allApps.size) return@withContext emptyList()

        val endIndex = minOf(pageIndex + pageSize, allApps.size)
        val chunk = allApps.subList(pageIndex, endIndex)

        val packageManager = context.packageManager
        chunk.map { resolveInfo ->
            AppInfo(
                appName = resolveInfo.loadLabel(packageManager).toString(),
                packageName = resolveInfo.activityInfo.packageName
            )
        }
    }

    override suspend fun getTotalAppCount(): Int = withContext(Dispatchers.IO) {
        getOrFetchRawApps().size
    }

    private fun getOrFetchRawApps(): List<ResolveInfo> {
        cachedResolveInfos?.let { return it }

        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = context.packageManager.queryIntentActivities(intent, 0)
            .sortedBy { it.activityInfo.packageName }

        cachedResolveInfos = apps
        return apps
    }
}